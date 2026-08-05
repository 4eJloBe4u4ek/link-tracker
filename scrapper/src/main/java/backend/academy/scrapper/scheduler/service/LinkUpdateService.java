package backend.academy.scrapper.scheduler.service;

import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.scrapper.scheduler.handler.GithubUpdateHandler;
import backend.academy.scrapper.scheduler.handler.PuppetTheatreUpdateHandler;
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
    private final PuppetTheatreUpdateHandler puppetTheatreHandler;
    private final LinkTypeResolver linkTypeResolver;
    private final ExecutorService executorService;

    public LinkUpdateService(
            ScrapperConfig scrapperConfig,
            LinkOperationRepository linkOperationRepository,
            GithubUpdateHandler githubHandler,
            StackOverflowUpdateHandler stackoverflowHandler,
            PuppetTheatreUpdateHandler puppetTheatreHandler,
            LinkTypeResolver linkTypeResolver) {
        this.scrapperConfig = scrapperConfig;
        this.linkOperationRepository = linkOperationRepository;
        this.githubHandler = githubHandler;
        this.stackoverflowHandler = stackoverflowHandler;
        this.puppetTheatreHandler = puppetTheatreHandler;
        this.linkTypeResolver = linkTypeResolver;
        this.executorService =
                Executors.newFixedThreadPool(scrapperConfig.scheduler().threadCount());
    }

    public void checkForUpdates() {
        int page = 0;
        int chunkSize = scrapperConfig.batchSize() / scrapperConfig.scheduler().threadCount();
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
                .map(subList ->
                        CompletableFuture.runAsync(() -> subList.forEach(this::checkLinkUpdates), executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    private void checkLinkUpdates(TrackedLink trackedLink) {
        String url = trackedLink.url();
        Optional<LinkType> linkType = linkTypeResolver.resolve(url);
        if (linkType.isEmpty()) {
            log.atWarn()
                    .setMessage("Unknown url pattern, skipping link")
                    .addKeyValue("url", url)
                    .log();
            return;
        }

        CompletableFuture<Void> updateFuture =
                switch (linkType.orElseThrow()) {
                    case GITHUB -> githubHandler.handle(trackedLink);
                    case STACKOVERFLOW -> stackoverflowHandler.handle(trackedLink);
                    case PUPPET_THEATRE -> puppetTheatreHandler.handle(trackedLink);
                };

        updateFuture.thenRun(() ->
                linkOperationRepository.updateLastCheckedTime(trackedLink, LocalDateTime.now(ZoneId.systemDefault())));
    }

    private List<List<TrackedLink>> partition(List<TrackedLink> list, int size) {
        List<List<TrackedLink>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
