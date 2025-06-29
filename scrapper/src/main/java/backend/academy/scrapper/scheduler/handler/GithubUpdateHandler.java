package backend.academy.scrapper.scheduler.handler;

import backend.academy.scrapper.client.github.GithubClient;
import backend.academy.scrapper.scheduler.service.NotificationService;
import backend.academy.scrapper.scheduler.util.MessageFormatter;
import backend.academy.shared.dto.TrackedLink;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubUpdateHandler {
    private final GithubClient githubClient;
    private final NotificationService notificationService;
    private static final Pattern GITHUB_LINK_PATTERN = Pattern.compile("^https://github\\.com/([\\w-]+)/([\\w-]+)/?.*");

    public static boolean isGithubLink(String url) {
        return GITHUB_LINK_PATTERN.matcher(url).matches();
    }

    public CompletableFuture<Void> handle(TrackedLink trackedLink) {
        Matcher matcher = GITHUB_LINK_PATTERN.matcher(trackedLink.url());
        if (!matcher.matches()) {
            log.atWarn()
                    .setMessage("URL didn't match GitHub pattern")
                    .addKeyValue("url", trackedLink.url())
                    .log();
            return CompletableFuture.completedFuture(null);
        }
        String owner = matcher.group(1);
        String repo = matcher.group(2);

        return githubClient
                .getRepositoryUpdates(owner, repo, trackedLink.updatedAt())
                .flatMap(githubUpdates -> {
                    if (githubUpdates == null) {
                        return Mono.empty();
                    }

                    return Flux.concat(
                                    Flux.fromIterable(githubUpdates.commits())
                                            .flatMap(githubCommit -> notificationService.sendUpdate(
                                                    trackedLink,
                                                    MessageFormatter.formatGithubCommit(githubCommit),
                                                    githubCommit
                                                            .commit()
                                                            .author()
                                                            .name())),
                                    Flux.fromIterable(githubUpdates.issues())
                                            .flatMap(githubIssue -> notificationService.sendUpdate(
                                                    trackedLink,
                                                    MessageFormatter.formatGithubIssue(githubIssue),
                                                    githubIssue.user().login())),
                                    Flux.fromIterable(githubUpdates.comments())
                                            .flatMap(githubComment -> notificationService.sendUpdate(
                                                    trackedLink,
                                                    MessageFormatter.formatGithubComment(githubComment),
                                                    githubComment.user().login())),
                                    Flux.fromIterable(githubUpdates.pullRequests())
                                            .flatMap(githubPullRequest -> notificationService.sendUpdate(
                                                    trackedLink,
                                                    MessageFormatter.formatGithubPullRequest(githubPullRequest),
                                                    githubPullRequest.user().login())))
                            .then();
                })
                .toFuture();
    }
}
