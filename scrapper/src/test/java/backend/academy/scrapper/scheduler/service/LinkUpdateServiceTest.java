package backend.academy.scrapper.scheduler.service;

import static backend.academy.scrapper.TestData.GITHUB_TRACKED_LINK;
import static backend.academy.scrapper.TestData.PUPPET_THEATRE_TRACKED_LINK;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_TRACKED_LINK;
import static backend.academy.scrapper.TestData.TICKETPRO_TRACKED_LINK;
import static backend.academy.scrapper.TestData.UNKNOWN_TRACKED_LINK;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.scrapper.scheduler.handler.GithubUpdateHandler;
import backend.academy.scrapper.scheduler.handler.PuppetTheatreUpdateHandler;
import backend.academy.scrapper.scheduler.handler.StackOverflowUpdateHandler;
import backend.academy.scrapper.scheduler.handler.TicketproUpdateHandler;
import backend.academy.scrapper.service.LinkTypeResolver;
import backend.academy.shared.dto.TrackedLink;
import java.util.ArrayList;
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

    @Mock
    private TicketproUpdateHandler ticketproHandler;

    @Mock
    private PuppetTheatreUpdateHandler puppetTheatreHandler;

    private LinkUpdateService linkUpdateService;

    @BeforeEach
    void setUp() {
        when(scrapperConfig.scheduler()).thenReturn(scheduler);
        when(scheduler.threadCount()).thenReturn(2);
        when(scrapperConfig.batchSize()).thenReturn(4);

        linkUpdateService = new LinkUpdateService(
                scrapperConfig,
                linkOperationRepository,
                githubHandler,
                stackoverflowHandler,
                ticketproHandler,
                puppetTheatreHandler,
                new LinkTypeResolver());
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
    void shouldCheckForTicketproUpdates() {
        // Arrange
        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(TICKETPRO_TRACKED_LINK));
        when(ticketproHandler.handle(TICKETPRO_TRACKED_LINK)).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        linkUpdateService.checkForUpdates();

        // Assert
        verify(ticketproHandler).handle(TICKETPRO_TRACKED_LINK);
        verify(linkOperationRepository).updateLastCheckedTime(eq(TICKETPRO_TRACKED_LINK), any());
    }

    @Test
    void shouldCheckDirectPuppetTheatreUpdates() {
        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(PUPPET_THEATRE_TRACKED_LINK));
        when(puppetTheatreHandler.handle(PUPPET_THEATRE_TRACKED_LINK))
                .thenReturn(CompletableFuture.completedFuture(null));

        linkUpdateService.checkForUpdates();

        verify(puppetTheatreHandler).handle(PUPPET_THEATRE_TRACKED_LINK);
        verify(linkOperationRepository).updateLastCheckedTime(eq(PUPPET_THEATRE_TRACKED_LINK), any());
    }

    @Test
    void shouldWaitForTicketproHandlerBeforeCompletingSchedulerCycle() {
        // Arrange
        CompletableFuture<Void> pendingHandler = new CompletableFuture<>();
        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(TICKETPRO_TRACKED_LINK));
        when(ticketproHandler.handle(TICKETPRO_TRACKED_LINK)).thenReturn(pendingHandler);

        // Act
        CompletableFuture<Void> schedulerCycle = CompletableFuture.runAsync(linkUpdateService::checkForUpdates);
        verify(ticketproHandler, timeout(1000)).handle(TICKETPRO_TRACKED_LINK);

        // Assert
        assertFalse(schedulerCycle.isDone());
        verify(linkOperationRepository, never()).updateLastCheckedTime(eq(TICKETPRO_TRACKED_LINK), any());

        // Act
        pendingHandler.complete(null);
        schedulerCycle.join();

        // Assert
        assertTrue(schedulerCycle.isDone());
        verify(linkOperationRepository).updateLastCheckedTime(eq(TICKETPRO_TRACKED_LINK), any());
    }

    @Test
    void shouldNotUpdateTicketproLastCheckedTimeWhenHandlerFails() {
        // Arrange
        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(TICKETPRO_TRACKED_LINK));
        when(ticketproHandler.handle(TICKETPRO_TRACKED_LINK))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("scrape failed")));

        // Act
        linkUpdateService.checkForUpdates();

        // Assert
        verify(ticketproHandler).handle(TICKETPRO_TRACKED_LINK);
        verify(linkOperationRepository, never()).updateLastCheckedTime(eq(TICKETPRO_TRACKED_LINK), any());
    }

    @Test
    void shouldIsolateFailedHandlerAndWaitForOtherLinks() {
        // Arrange
        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(GITHUB_TRACKED_LINK, TICKETPRO_TRACKED_LINK));
        when(githubHandler.handle(GITHUB_TRACKED_LINK))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("github failed")));
        when(ticketproHandler.handle(TICKETPRO_TRACKED_LINK)).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        linkUpdateService.checkForUpdates();

        // Assert
        verify(githubHandler).handle(GITHUB_TRACKED_LINK);
        verify(ticketproHandler).handle(TICKETPRO_TRACKED_LINK);
        verify(linkOperationRepository, never()).updateLastCheckedTime(eq(GITHUB_TRACKED_LINK), any());
        verify(linkOperationRepository).updateLastCheckedTime(eq(TICKETPRO_TRACKED_LINK), any());
    }

    @Test
    void shouldSkipUnknownLinks() {
        // Arrange
        when(linkOperationRepository.getAllLinks(0)).thenReturn(List.of(UNKNOWN_TRACKED_LINK));

        // Act
        linkUpdateService.checkForUpdates();

        // Assert
        verifyNoInteractions(githubHandler, stackoverflowHandler, ticketproHandler, puppetTheatreHandler);
        verify(linkOperationRepository, never()).updateLastCheckedTime(eq(UNKNOWN_TRACKED_LINK), any());
    }

    @Test
    void shouldProcessLinksWhenThreadCountExceedsBatchSize() {
        // Arrange
        when(scrapperConfig.batchSize()).thenReturn(1);
        when(scheduler.threadCount()).thenReturn(4);
        linkUpdateService = new LinkUpdateService(
                scrapperConfig,
                linkOperationRepository,
                githubHandler,
                stackoverflowHandler,
                ticketproHandler,
                puppetTheatreHandler,
                new LinkTypeResolver());
        List<TrackedLink> trackedLinks = new PartitionProgressList(GITHUB_TRACKED_LINK);
        when(linkOperationRepository.getAllLinks(0)).thenReturn(trackedLinks);
        when(githubHandler.handle(GITHUB_TRACKED_LINK)).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        linkUpdateService.checkForUpdates();

        // Assert
        verify(githubHandler).handle(GITHUB_TRACKED_LINK);
        verify(linkOperationRepository).updateLastCheckedTime(eq(GITHUB_TRACKED_LINK), any());
    }

    private static final class PartitionProgressList extends ArrayList<TrackedLink> {
        private PartitionProgressList(TrackedLink trackedLink) {
            super(List.of(trackedLink));
        }

        @Override
        public List<TrackedLink> subList(int fromIndex, int toIndex) {
            if (fromIndex == toIndex) {
                throw new AssertionError("Partition size must advance the cursor");
            }
            return super.subList(fromIndex, toIndex);
        }
    }
}
