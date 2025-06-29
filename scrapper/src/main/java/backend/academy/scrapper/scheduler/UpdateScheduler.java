package backend.academy.scrapper.scheduler;

import backend.academy.scrapper.botclient.BotClient;
import backend.academy.scrapper.client.github.GithubClient;
import backend.academy.scrapper.client.github.GithubComment;
import backend.academy.scrapper.client.github.GithubCommit;
import backend.academy.scrapper.client.github.GithubIssue;
import backend.academy.scrapper.client.stackoverflow.StackoverflowClient;
import backend.academy.scrapper.repository.InMemoryRepository;
import backend.academy.shared.dto.LinkUpdate;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

@Controller
public class UpdateScheduler {
    private final BotClient botClient;
    private final GithubClient githubClient;
    private final StackoverflowClient stackoverflowClient;
    private final InMemoryRepository inMemoryRepository;
    private final AtomicLong linkUpdateIdGenerator = new AtomicLong(1);

    public UpdateScheduler(
            GithubClient githubClient,
            StackoverflowClient stackoverflowClient,
            BotClient botClient,
            InMemoryRepository inMemoryRepository) {
        this.githubClient = githubClient;
        this.stackoverflowClient = stackoverflowClient;
        this.botClient = botClient;
        this.inMemoryRepository = inMemoryRepository;
    }

    @Scheduled(fixedRate = 30_000)
    public void checkUpdates() {
        Map<String, TrackedLink> uniqueTrackedLinks = inMemoryRepository.trackedLinks().values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(TrackedLink::url, trackedLink -> trackedLink, (tl1, tl2) -> tl1));

        for (TrackedLink trackedLink : uniqueTrackedLinks.values()) {
            checkLinkUpdates(trackedLink);
        }
    }

    private void checkLinkUpdates(TrackedLink trackedLink) {
        String url = trackedLink.url();
        if (url.contains("github")) {
            handleGithubUpdate(trackedLink);
        } else if (url.contains("stackoverflow")) {
            handleStackoverflowUpdate(trackedLink);
        }
    }

    private void handleGithubUpdate(TrackedLink trackedLink) {
        String[] parts = trackedLink.url().split("/");
        String owner = parts[parts.length - 2];
        String repo = parts[parts.length - 1];

        githubClient.getRepositoryUpdates(owner, repo).subscribe(githubUpdates -> {
            List<GithubCommit> newCommits = githubUpdates.commits().stream()
                    .filter(githubCommit ->
                            githubCommit.commit().committer().date().isAfter(trackedLink.updatedAt()))
                    .toList();
            List<GithubIssue> newIssues = githubUpdates.issues().stream()
                    .filter(githubIssue -> githubIssue.createdAt().isAfter(trackedLink.updatedAt()))
                    .toList();

            List<GithubComment> newComments = githubUpdates.comments().stream()
                    .filter(githubComment -> githubComment.createdAt().isAfter(trackedLink.updatedAt()))
                    .toList();

            List<Long> tgChatIds = inMemoryRepository.getChatsForTrackedLink(trackedLink.url());

            for (GithubCommit githubCommit : newCommits) {
                botClient
                        .updateLink(new LinkUpdate(
                                linkUpdateIdGenerator.getAndIncrement(),
                                trackedLink.url(),
                                githubCommit.commit().message(),
                                tgChatIds))
                        .subscribe();
            }

            for (GithubIssue githubIssue : newIssues) {
                botClient
                        .updateLink(new LinkUpdate(
                                linkUpdateIdGenerator.getAndIncrement(),
                                trackedLink.url(),
                                githubIssue.title(),
                                tgChatIds))
                        .subscribe();
            }

            for (GithubComment githubComment : newComments) {
                botClient
                        .updateLink(new LinkUpdate(
                                linkUpdateIdGenerator.getAndIncrement(),
                                trackedLink.url(),
                                githubComment.body(),
                                tgChatIds))
                        .subscribe();
            }

            inMemoryRepository.updateLastCheckedTime(trackedLink, LocalDateTime.now());
        });
    }

    private void handleStackoverflowUpdate(TrackedLink trackedLink) {
        String question = trackedLink.url().substring(trackedLink.url().lastIndexOf("/") + 1);
        stackoverflowClient.getQuestion(Long.parseLong(question)).subscribe(questionUpdates -> {
            List<Long> tgChatIds = inMemoryRepository.getChatsForTrackedLink(trackedLink.url());
            if (questionUpdates.lastActivityDate().isAfter(trackedLink.updatedAt())) {
                botClient
                        .updateLink(new LinkUpdate(
                                linkUpdateIdGenerator.getAndIncrement(),
                                trackedLink.url(),
                                questionUpdates.title(),
                                tgChatIds))
                        .subscribe();
            }

            inMemoryRepository.updateLastCheckedTime(trackedLink, LocalDateTime.now());
        });
    }
}
