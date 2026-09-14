package backend.academy.scrapper.scheduler.service;

import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.scrapper.scheduler.handler.GithubUpdateHandler;
import backend.academy.scrapper.scheduler.handler.TicketproUpdateHandler;
import backend.academy.scrapper.scheduler.handler.StackOverflowUpdateHandler;
import backend.academy.scrapper.service.LinkTypeResolver;
import backend.academy.shared.dto.LinkType;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LinkUpdateService {
    private final ScrapperConfig scrapperConfig;
    private final LinkOperationRepository linkOperationRepository;
    private final GithubUpdateHandler githubHandler;
    private final StackOverflowUpdateHandler stackoverflowHandler;
    private final TicketproUpdateHandler ticketproHandler;
    private final LinkTypeResolver linkTypeResolver;
    private final ExecutorService executorService;

    public LinkUpdateService(
            ScrapperConfig scrapperConfig,
            LinkOperationRepository linkOperationRepository,
            GithubUpdateHandler githubHandler,
            StackOverflowUpdateHandler stackoverflowHandler,
            TicketproUpdateHandler ticketproHandler,
            LinkTypeResolver linkTypeResolver) {
        this.scrapperConfig = scrapperConfig;
        this.linkOperationRepository = linkOperationRepository;
        this.githubHandler = githubHandler;
        this.stackoverflowHandler = stackoverflowHandler;
        this.ticketproHandler = ticketproHandler;
        this.linkTypeResolver = linkTypeResolver;
        this.executorService =
                Executors.newFixedThreadPool(scrapperConfig.scheduler().threadCount());
    }

    public void checkForUpdates() {
        int page = 0;
        int chunkSize = Math.max(1, scrapperConfig.batchSize() / scrapperConfig.scheduler().threadCount());
        List<TrackedLink> trackedLinks;
        do {
            trackedLinks = linkOperationRepository.getAllLinks(page++);
            if (!trackedLinks.isEmpty()) {
                processLinksMultithreaded(trackedLinks, chunkSize);
            }
        } while (!trackedLinks.isEmpty());
    }

    private void processLinksMultithreaded(List<TrackedLink> trackedLinks, int chunkSize) {
        List<List<TrackedLink>> partitions = partition(trackedLinks, chunkSize);

        List<CompletableFuture<Void>> futures = partitions.stream()
                .map(subList -> CompletableFuture.runAsync(
                        () -> CompletableFuture.allOf(subList.stream()
                                        .map(this::checkLinkUpdates)
                                        .toArray(CompletableFuture[]::new))
                                .join(),
                        executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    private CompletableFuture<Void> checkLinkUpdates(TrackedLink trackedLink) {
        try {
            String url = trackedLink.url();
            Optional<LinkType> linkType = linkTypeResolver.resolve(url);
            if (linkType.isEmpty()) {
                log.atWarn()
                        .setMessage("Unknown url pattern, skipping link")
                        .addKeyValue("url", url)
                        .log();
                return CompletableFuture.completedFuture(null);
            }

            CompletableFuture<Void> updateFuture =
                    switch (linkType.orElseThrow()) {
                        case GITHUB -> githubHandler.handle(trackedLink);
                        case STACKOVERFLOW -> stackoverflowHandler.handle(trackedLink);
                        case TICKETPRO -> ticketproHandler.handle(trackedLink);
                    };

            return updateFuture
                    .thenRun(() -> linkOperationRepository.updateLastCheckedTime(
                            trackedLink, LocalDateTime.now(ZoneId.systemDefault())))
                    .exceptionally(error -> {
                        logUpdateFailure(trackedLink, error);
                        return null;
                    });
        } catch (RuntimeException error) {
            logUpdateFailure(trackedLink, error);
            return CompletableFuture.completedFuture(null);
        }
    }

    private void logUpdateFailure(TrackedLink trackedLink, Throwable error) {
        log.atError()
                .setMessage("Failed to update tracked link")
                .addKeyValue("url", trackedLink.url())
                .setCause(error)
                .log();
    }

    private List<List<TrackedLink>> partition(List<TrackedLink> list, int size) {
        List<List<TrackedLink>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
