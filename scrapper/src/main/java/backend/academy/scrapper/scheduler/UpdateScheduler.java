package backend.academy.scrapper.scheduler;

import backend.academy.scrapper.client.github.GithubClient;
import backend.academy.scrapper.client.stackoverflow.StackoverflowClient;
import backend.academy.scrapper.client.stackoverflow.StackoverflowQuestion;
import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.scrapper.scheduler.util.MessageFormatter;
import backend.academy.shared.dto.LinkUpdate;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class UpdateScheduler {
    private final ScrapperConfig scrapperConfig;
    private final GithubClient githubClient;
    private final StackoverflowClient stackoverflowClient;
    private final LinkOperationRepository linkOperationRepository;
    private final UpdateSender updateSender;
    private final AtomicLong linkUpdateIdGenerator = new AtomicLong(1);
    private final ExecutorService executorService;

    private static final Pattern GITHUB_LINK_PATTERN = Pattern.compile("^https://github\\.com/([\\w-]+)/([\\w-]+)/?.*");
    private static final Pattern STACKOVERFLOW_LINK_PATTERN =
            Pattern.compile("^https://stackoverflow\\.com/questions/(\\d+)/?.*");

    public UpdateScheduler(
            ScrapperConfig scrapperConfig,
            GithubClient githubClient,
            StackoverflowClient stackoverflowClient,
            LinkOperationRepository linkOperationRepository,
            UpdateSender updateSender) {
        this.scrapperConfig = scrapperConfig;
        this.githubClient = githubClient;
        this.stackoverflowClient = stackoverflowClient;
        this.linkOperationRepository = linkOperationRepository;
        this.updateSender = updateSender;
        this.executorService =
                Executors.newFixedThreadPool(scrapperConfig.scheduler().threadCount());
    }

    @Scheduled(fixedDelayString = "#{scheduler.interval()}")
    public void checkUpdates() {
        log.atInfo().setMessage("Checking updates...").log();

        int page = 0;
        int chunkSize = scrapperConfig.batchSize() / scrapperConfig.scheduler().threadCount();
        List<TrackedLink> trackedLinks;
        do {
            trackedLinks = linkOperationRepository.getAllLinks(page++);
            if (!trackedLinks.isEmpty()) {
                log.atInfo()
                        .setMessage("Processing batch of links")
                        .addKeyValue("batchSize", trackedLinks.size())
                        .log();
                processLinksMultithreaded(trackedLinks, chunkSize);
            }
        } while (!trackedLinks.isEmpty());

        log.atInfo().setMessage("Finished checking updates").log();
    }

    private void processLinksMultithreaded(List<TrackedLink> trackedLinks, int chunkSize) {
        List<List<TrackedLink>> partitions = new ArrayList<>();
        for (int i = 0; i < trackedLinks.size(); i += chunkSize) {
            partitions.add(trackedLinks.subList(i, Math.min(i + chunkSize, trackedLinks.size())));
        }

        List<CompletableFuture<Void>> futures = partitions.stream()
                .map(subList ->
                        CompletableFuture.runAsync(() -> subList.forEach(this::checkLinkUpdates), executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    private void checkLinkUpdates(TrackedLink trackedLink) {
        String url = trackedLink.url();
        Matcher githubMatcher = GITHUB_LINK_PATTERN.matcher(url);
        Matcher stackoverflowMatcher = STACKOVERFLOW_LINK_PATTERN.matcher(url);

        CompletableFuture<Void> updateFuture;

        if (githubMatcher.matches()) {
            String owner = githubMatcher.group(1);
            String repo = githubMatcher.group(2);
            updateFuture = handleGithubUpdate(trackedLink, owner, repo);
        } else if (stackoverflowMatcher.matches()) {
            Long questionId = Long.parseLong(stackoverflowMatcher.group(1));
            updateFuture = handleStackoverflowUpdate(trackedLink, questionId);
        } else {
            log.atWarn()
                    .setMessage("Unknown url pattern, skipping link")
                    .addKeyValue("url", url)
                    .log();
            return;
        }

        updateFuture.thenRun(() ->
                linkOperationRepository.updateLastCheckedTime(trackedLink, LocalDateTime.now(ZoneId.systemDefault())));
    }

    private CompletableFuture<Void> handleGithubUpdate(TrackedLink trackedLink, String owner, String repo) {
        return githubClient
                .getRepositoryUpdates(owner, repo, trackedLink.updatedAt())
                .flatMap(githubUpdates -> {
                    if (githubUpdates == null) {
                        return Mono.empty();
                    }

                    List<Long> tgChatIds = getTgChatIds(trackedLink);

                    return Flux.concat(
                                    Flux.fromIterable(githubUpdates.commits())
                                            .flatMap(githubCommit -> sendUpdate(
                                                    trackedLink,
                                                    tgChatIds,
                                                    MessageFormatter.formatGithubCommit(githubCommit))),
                                    Flux.fromIterable(githubUpdates.issues())
                                            .flatMap(githubIssue -> sendUpdate(
                                                    trackedLink,
                                                    tgChatIds,
                                                    MessageFormatter.formatGithubIssue(githubIssue))),
                                    Flux.fromIterable(githubUpdates.comments())
                                            .flatMap(githubComment -> sendUpdate(
                                                    trackedLink,
                                                    tgChatIds,
                                                    MessageFormatter.formatGithubComment(githubComment))),
                                    Flux.fromIterable(githubUpdates.pullRequests())
                                            .flatMap(githubPullRequest -> sendUpdate(
                                                    trackedLink,
                                                    tgChatIds,
                                                    MessageFormatter.formatGithubPullRequest(githubPullRequest))))
                            .then();
                })
                .toFuture();
    }

    private CompletableFuture<Void> handleStackoverflowUpdate(TrackedLink trackedLink, Long questionId) {
        return stackoverflowClient
                .getStackoverflowUpdates(questionId, trackedLink.updatedAt())
                .flatMap(stackoverflowUpdates -> {
                    if (stackoverflowUpdates == null) {
                        return Mono.empty();
                    }

                    List<Long> tgChatIds = getTgChatIds(trackedLink);
                    StackoverflowQuestion stackoverflowQuestion = stackoverflowUpdates.question();

                    return Flux.concat(
                                    Flux.just(stackoverflowQuestion)
                                            .filter(question ->
                                                    question.lastActivityDate().isAfter(trackedLink.updatedAt()))
                                            .flatMap(question -> sendUpdate(
                                                    trackedLink,
                                                    tgChatIds,
                                                    MessageFormatter.formatStackoverflowQuestion(question))),
                                    Flux.fromIterable(stackoverflowUpdates.answers())
                                            .flatMap(stackoverflowAnswer -> sendUpdate(
                                                    trackedLink,
                                                    tgChatIds,
                                                    MessageFormatter.formatStackoverflowAnswer(
                                                            stackoverflowQuestion, stackoverflowAnswer))),
                                    Flux.fromIterable(stackoverflowUpdates.commentsToQuestion())
                                            .flatMap(stackoverflowComment -> sendUpdate(
                                                    trackedLink,
                                                    tgChatIds,
                                                    MessageFormatter.formatStackoverflowComment(
                                                            stackoverflowQuestion, stackoverflowComment))),
                                    Flux.fromIterable(stackoverflowUpdates.commentsToAnswers())
                                            .flatMap(stackoverflowComment -> sendUpdate(
                                                    trackedLink,
                                                    tgChatIds,
                                                    MessageFormatter.formatStackoverflowComment(
                                                            stackoverflowQuestion, stackoverflowComment))))
                            .then();
                })
                .toFuture();
    }

    private Mono<Void> sendUpdate(TrackedLink trackedLink, List<Long> tgChatIds, String message) {
        return updateSender.sendUpdate(
                new LinkUpdate(linkUpdateIdGenerator.getAndIncrement(), trackedLink.url(), message, tgChatIds));
    }

    private List<Long> getTgChatIds(TrackedLink trackedLink) {
        int page = 0;
        List<Long> tgChatIds = new ArrayList<>();
        List<Long> batch;
        do {
            batch = linkOperationRepository.getChatsForLink(trackedLink, page++);
            tgChatIds.addAll(batch);
        } while (!batch.isEmpty());

        return tgChatIds;
    }
}
