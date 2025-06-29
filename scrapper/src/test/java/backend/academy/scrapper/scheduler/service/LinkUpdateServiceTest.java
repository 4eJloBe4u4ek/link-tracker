package backend.academy.scrapper.scheduler.service;

import static backend.academy.scrapper.TestData.GITHUB_TRACKED_LINK;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_TRACKED_LINK;
import static backend.academy.scrapper.TestData.UNKNOWN_TRACKED_LINK;
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
        // Arrange
        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(GITHUB_TRACKED_LINK));
        when(githubHandler.handle(GITHUB_TRACKED_LINK)).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        linkUpdateService.checkForUpdates();

        // Assert
        verify(githubHandler).handle(GITHUB_TRACKED_LINK);
        verify(linkOperationRepository).updateLastCheckedTime(eq(GITHUB_TRACKED_LINK), any());
    }

    @Test
    void shouldCheckForStackOverflowUpdates() {
        // Arrange
        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(STACKOVERFLOW_TRACKED_LINK));
        when(stackoverflowHandler.handle(STACKOVERFLOW_TRACKED_LINK))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        linkUpdateService.checkForUpdates();

        // Assert
        verify(stackoverflowHandler).handle(STACKOVERFLOW_TRACKED_LINK);
        verify(linkOperationRepository).updateLastCheckedTime(eq(STACKOVERFLOW_TRACKED_LINK), any());
    }

    @Test
    void shouldSkipUnknownLinks() {
        // Arrange
        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(UNKNOWN_TRACKED_LINK));

        // Act
        linkUpdateService.checkForUpdates();

        // Assert
        verifyNoInteractions(githubHandler, stackoverflowHandler);
        verify(linkOperationRepository, never()).updateLastCheckedTime(eq(UNKNOWN_TRACKED_LINK), any());
    }
}
