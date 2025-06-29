package backend.academy.scrapper.scheduler.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.scrapper.scheduler.handler.GithubUpdateHandler;
import backend.academy.scrapper.scheduler.handler.StackOverflowUpdateHandler;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkUpdateServiceTest {

    @Mock
    private ScrapperConfig scrapperConfig;

    @Mock
    private ScrapperConfig.Scheduler scheduler;

    @Mock
    private LinkOperationRepository linkOperationRepository;

    @Mock
    private GithubUpdateHandler githubHandler;

    @Mock
    private StackOverflowUpdateHandler stackoverflowHandler;

    private LinkUpdateService linkUpdateService;

    @BeforeEach
    void setUp() {
        when(scrapperConfig.scheduler()).thenReturn(scheduler);
        when(scheduler.threadCount()).thenReturn(2);
        when(scrapperConfig.batchSize()).thenReturn(4);

        linkUpdateService =
                new LinkUpdateService(scrapperConfig, linkOperationRepository, githubHandler, stackoverflowHandler);
    }

    @Test
    void shouldCheckForGithubUpdates() {
        TrackedLink githubLink = new TrackedLink(
                1L, "https://github.com/owner/repo", List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now());
        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(githubLink));
        when(githubHandler.handle(githubLink)).thenReturn(CompletableFuture.completedFuture(null));

        linkUpdateService.checkForUpdates();

        verify(githubHandler).handle(githubLink);
        verify(linkOperationRepository).updateLastCheckedTime(eq(githubLink), any());
    }

    @Test
    void shouldCheckForStackOverflowUpdates() {
        TrackedLink stackOverflowLink = new TrackedLink(
                2L,
                "https://stackoverflow.com/questions/123",
                List.of(),
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now());
        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(stackOverflowLink));
        when(stackoverflowHandler.handle(stackOverflowLink)).thenReturn(CompletableFuture.completedFuture(null));

        linkUpdateService.checkForUpdates();

        verify(stackoverflowHandler).handle(stackOverflowLink);
        verify(linkOperationRepository).updateLastCheckedTime(eq(stackOverflowLink), any());
    }

    @Test
    void shouldSkipUnknownLinks() {
        TrackedLink unknownLink = new TrackedLink(
                3L, "https://unknown.com/resource", List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now());
        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(unknownLink));

        linkUpdateService.checkForUpdates();

        verifyNoInteractions(githubHandler, stackoverflowHandler);
        verify(linkOperationRepository, never()).updateLastCheckedTime(eq(unknownLink), any());
    }
}
