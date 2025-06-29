package backend.academy.scrapper.scheduler;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.github.GithubClient;
import backend.academy.scrapper.client.github.GithubCommit;
import backend.academy.scrapper.client.github.GithubRepositoryUpdates;
import backend.academy.scrapper.client.stackoverflow.StackoverflowClient;
import backend.academy.scrapper.client.stackoverflow.StackoverflowOwner;
import backend.academy.scrapper.client.stackoverflow.StackoverflowQuestion;
import backend.academy.scrapper.client.stackoverflow.StackoverflowQuestionUpdates;
import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class UpdateSchedulerTest {

    @Mock
    private GithubClient githubClient;

    @Mock
    private StackoverflowClient stackoverflowClient;

    @Mock
    private UpdateSender updateSender;

    @Mock
    private LinkOperationRepository linkOperationRepository;

    @Mock
    private ScrapperConfig scrapperConfig;

    @Mock
    private ScrapperConfig.Scheduler schedulerConfig;

    private UpdateScheduler scheduler;

    @BeforeEach
    public void setup() {
        when(scrapperConfig.scheduler()).thenReturn(schedulerConfig);
        when(schedulerConfig.threadCount()).thenReturn(4);
        when(scrapperConfig.batchSize()).thenReturn(100);

        scheduler = new UpdateScheduler(
                scrapperConfig, githubClient, stackoverflowClient, linkOperationRepository, updateSender);
    }

    @Test
    void sendsNotificationsOnlyToSubscribedUsersForGithubUpdates() {
        LocalDateTime now = LocalDateTime.now();
        TrackedLink activeLink = new TrackedLink(1L, "https://github.com/owner/repo", List.of(), List.of(), now, now);

        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(activeLink));
        when(linkOperationRepository.getChatsForLink(activeLink, 0)).thenReturn(List.of(1L));
        GithubCommit newCommit = new GithubCommit(new GithubCommit.Commit(
                new GithubCommit.Author(12345L, "author", now.plusHours(1)), "commit-url", "New commit"));
        when(githubClient.getRepositoryUpdates("owner", "repo", activeLink.updatedAt()))
                .thenReturn(
                        Mono.just(new GithubRepositoryUpdates(List.of(newCommit), List.of(), List.of(), List.of())));

        scheduler.checkUpdates();

        verify(updateSender)
                .sendUpdate(argThat(update -> update.url().equals(activeLink.url())
                        && update.tgChatIds().equals(List.of(1L))));
        verifyNoMoreInteractions(updateSender);
    }

    @Test
    void sendsNotificationsOnlyToSubscribedUsersForStackOverflowUpdates() {
        LocalDateTime now = LocalDateTime.now();
        TrackedLink activeLink =
                new TrackedLink(1L, "https://stackoverflow.com/questions/123456", List.of(), List.of(), now, now);

        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(activeLink));
        when(linkOperationRepository.getChatsForLink(activeLink, 0)).thenReturn(List.of(1L));
        StackoverflowOwner owner = new StackoverflowOwner(1L, "TestUser", 100L, 12345L);
        StackoverflowQuestion updatedQuestion = new StackoverflowQuestion(
                123456L,
                owner,
                now.minusDays(2),
                now.plusHours(1),
                now.plusHours(1),
                "Updated Question Title",
                true,
                5);
        when(stackoverflowClient.getStackoverflowUpdates(123456L, activeLink.updatedAt()))
                .thenReturn(
                        Mono.just(new StackoverflowQuestionUpdates(updatedQuestion, List.of(), List.of(), List.of())));

        scheduler.checkUpdates();

        verify(updateSender)
                .sendUpdate(argThat(update -> update.url().equals(activeLink.url())
                        && update.tgChatIds().equals(List.of(1L))));
        verifyNoMoreInteractions(updateSender);
    }
}
