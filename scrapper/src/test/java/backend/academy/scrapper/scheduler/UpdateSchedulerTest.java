package backend.academy.scrapper.scheduler;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.botclient.BotClient;
import backend.academy.scrapper.client.github.GithubClient;
import backend.academy.scrapper.client.github.GithubCommit;
import backend.academy.scrapper.client.github.GithubRepositoryUpdates;
import backend.academy.scrapper.client.stackoverflow.StackoverflowClient;
import backend.academy.scrapper.repository.InMemoryRepository;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    private BotClient botClient;

    @Mock
    private InMemoryRepository repository;

    @InjectMocks
    private UpdateScheduler scheduler;

    @Test
    void sendsNotificationsOnlyToSubscribedUsers() {
        TrackedLink activeLink = new TrackedLink(
                1L,
                "https://github.com/owner/repo",
                List.of(),
                List.of(),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1));

        when(repository.trackedLinks()).thenReturn(Map.of(1L, List.of(activeLink)));

        when(repository.getChatsForTrackedLink(activeLink.url())).thenReturn(List.of(1L));

        GithubCommit newCommit = new GithubCommit(new GithubCommit.Commit(
                new GithubCommit.Committer("author", LocalDateTime.now()), "commit-url", "New commit"));

        when(githubClient.getRepositoryUpdates("owner", "repo"))
                .thenReturn(Mono.just(new GithubRepositoryUpdates(List.of(newCommit), List.of(), List.of())));

        scheduler.checkUpdates();

        verify(botClient)
                .updateLink(argThat(update -> update.url().equals(activeLink.url())
                        && update.tgChatIds().equals(List.of(1L))));

        verifyNoMoreInteractions(botClient);
    }
}
