package backend.academy.scrapper.scheduler.handler;

import static backend.academy.scrapper.TestData.PUPPET_THEATRE_TRACKED_LINK;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.puppettheatre.PuppetTheatreClient;
import backend.academy.scrapper.client.puppettheatre.TicketproCheckResult;
import backend.academy.scrapper.client.puppettheatre.TicketproException;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreSession;
import backend.academy.scrapper.monitoring.HealthchecksClient;
import backend.academy.scrapper.monitoring.TicketproMetrics;
import backend.academy.scrapper.repository.PuppetTheatreSnapshotRepository;
import backend.academy.scrapper.scheduler.service.NotificationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@ExtendWith(MockitoExtension.class)
class PuppetTheatreUpdateHandlerTest {
    private static final PuppetTheatreSession SESSION = new PuppetTheatreSession(
            "Кот в сапогах",
            "12.09.2026",
            "11:00",
            "38–40 BYN",
            "https://www.ticketpro.by/bilety-v-teatr/kot-v-sapogah/");

    @Mock
    private PuppetTheatreClient client;

    @Mock
    private PuppetTheatreSnapshotRepository snapshotRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TicketproMetrics ticketproMetrics;

    @Mock
    private HealthchecksClient healthchecksClient;

    private PuppetTheatreUpdateHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PuppetTheatreUpdateHandler(
                client,
                snapshotRepository,
                notificationService,
                new SimpleMeterRegistry(),
                ticketproMetrics,
                healthchecksClient);
    }

    @Test
    void shouldSkipOverlappingCheck() {
        // Arrange
        Sinks.One<List<PuppetTheatreSession>> firstCheck = Sinks.one();
        when(client.getAvailableSessions()).thenReturn(firstCheck.asMono());

        // Act
        CompletableFuture<Void> activeCheck = handler.handle(PUPPET_THEATRE_TRACKED_LINK);
        CompletableFuture<Void> overlappingCheck = handler.handle(PUPPET_THEATRE_TRACKED_LINK);

        // Assert
        assertFalse(activeCheck.isDone());
        assertTrue(overlappingCheck.isDone());
        verify(client).getAvailableSessions();
    }

    @Test
    void shouldAllowNextCheckAfterSuccess() {
        // Arrange
        when(client.getAvailableSessions()).thenReturn(Mono.just(List.of()));
        when(snapshotRepository.getAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id()))
                .thenReturn(Optional.empty());

        // Act
        handler.handle(PUPPET_THEATRE_TRACKED_LINK).join();
        handler.handle(PUPPET_THEATRE_TRACKED_LINK).join();

        // Assert
        verify(client, times(2)).getAvailableSessions();
    }

    @Test
    void shouldAllowNextCheckAfterFailure() {
        // Arrange
        Sinks.One<List<PuppetTheatreSession>> firstCheck = Sinks.one();
        when(client.getAvailableSessions()).thenReturn(firstCheck.asMono(), Mono.just(List.of()));
        when(snapshotRepository.getAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id()))
                .thenReturn(Optional.empty());
        CompletableFuture<Void> failedCheck = handler.handle(PUPPET_THEATRE_TRACKED_LINK);

        // Act & Assert
        assertTrue(firstCheck
                .tryEmitError(new TicketproException(TicketproCheckResult.NETWORK_ERROR, "request failed"))
                .isSuccess());
        assertThrows(CompletionException.class, failedCheck::join);
        handler.handle(PUPPET_THEATRE_TRACKED_LINK).join();
        verify(client, times(2)).getAvailableSessions();
    }

    @Test
    void shouldInitializeSnapshotWithoutSendingOldTickets() {
        // Arrange
        when(client.getAvailableSessions()).thenReturn(Mono.just(List.of(SESSION)));
        when(snapshotRepository.getAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id()))
                .thenReturn(Optional.empty());

        // Act
        handler.handle(PUPPET_THEATRE_TRACKED_LINK).join();

        // Assert
        verify(notificationService, never()).sendUpdate(eq(PUPPET_THEATRE_TRACKED_LINK), anyString());
        verify(snapshotRepository)
                .replaceAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id(), Set.of(SESSION.snapshotKey()));
        InOrder completionOrder = inOrder(snapshotRepository, ticketproMetrics, healthchecksClient);
        completionOrder
                .verify(snapshotRepository)
                .replaceAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id(), Set.of(SESSION.snapshotKey()));
        completionOrder.verify(ticketproMetrics).recordSuccess(1);
        completionOrder.verify(healthchecksClient).pingTicketpro();
    }

    @Test
    void shouldNotifyWhenTicketsBecomeAvailable() {
        // Arrange
        when(client.getAvailableSessions()).thenReturn(Mono.just(List.of(SESSION)));
        when(snapshotRepository.getAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id()))
                .thenReturn(Optional.of(Set.of()));
        when(notificationService.sendUpdate(eq(PUPPET_THEATRE_TRACKED_LINK), anyString()))
                .thenReturn(Mono.empty());

        // Act
        handler.handle(PUPPET_THEATRE_TRACKED_LINK).join();

        // Assert
        verify(notificationService).sendUpdate(eq(PUPPET_THEATRE_TRACKED_LINK), anyString());
        verify(snapshotRepository)
                .replaceAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id(), Set.of(SESSION.snapshotKey()));
    }

    @Test
    void shouldKeepSnapshotAndNotNotifyWhenAvailableSessionsDoNotChange() {
        // Arrange
        when(client.getAvailableSessions()).thenReturn(Mono.just(List.of(SESSION)));
        when(snapshotRepository.getAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id()))
                .thenReturn(Optional.of(Set.of(SESSION.snapshotKey())));

        // Act
        handler.handle(PUPPET_THEATRE_TRACKED_LINK).join();

        // Assert
        verify(notificationService, never()).sendUpdate(eq(PUPPET_THEATRE_TRACKED_LINK), anyString());
        verify(snapshotRepository)
                .replaceAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id(), Set.of(SESSION.snapshotKey()));
    }

    @Test
    void shouldKeepSnapshotWhenNotificationFails() {
        // Arrange
        when(client.getAvailableSessions()).thenReturn(Mono.just(List.of(SESSION)));
        when(snapshotRepository.getAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id()))
                .thenReturn(Optional.of(Set.of()));
        when(notificationService.sendUpdate(eq(PUPPET_THEATRE_TRACKED_LINK), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("delivery failed")));

        // Act & Assert
        assertThrows(CompletionException.class, () -> handler.handle(PUPPET_THEATRE_TRACKED_LINK)
                .join());
        verify(snapshotRepository, never())
                .replaceAvailableSessionKeys(eq(PUPPET_THEATRE_TRACKED_LINK.id()), anySet());
        verify(ticketproMetrics).recordFailure(TicketproCheckResult.DELIVERY_ERROR);
        verifyNoInteractions(healthchecksClient);
    }

    @Test
    void shouldNotifyWhenSessionReappearsAfterBeingUnavailable() {
        // Arrange
        when(client.getAvailableSessions()).thenReturn(Mono.just(List.of()), Mono.just(List.of(SESSION)));
        when(snapshotRepository.getAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id()))
                .thenReturn(Optional.of(Set.of(SESSION.snapshotKey())), Optional.of(Set.of()));
        when(notificationService.sendUpdate(eq(PUPPET_THEATRE_TRACKED_LINK), anyString()))
                .thenReturn(Mono.empty());

        // Act
        handler.handle(PUPPET_THEATRE_TRACKED_LINK).join();
        handler.handle(PUPPET_THEATRE_TRACKED_LINK).join();

        // Assert
        InOrder verificationOrder = inOrder(snapshotRepository, notificationService);
        verificationOrder
                .verify(snapshotRepository)
                .replaceAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id(), Set.of());
        verificationOrder
                .verify(notificationService)
                .sendUpdate(eq(PUPPET_THEATRE_TRACKED_LINK), contains(SESSION.title()));
        verificationOrder
                .verify(snapshotRepository)
                .replaceAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id(), Set.of(SESSION.snapshotKey()));
    }

    @Test
    void shouldKeepSnapshotWhenCatalogCheckFails() {
        // Arrange
        when(client.getAvailableSessions())
                .thenReturn(Mono.error(new TicketproException(TicketproCheckResult.ANTIBOT, "bot-protection")));

        // Act & Assert
        assertThrows(CompletionException.class, () -> handler.handle(PUPPET_THEATRE_TRACKED_LINK)
                .join());
        verify(snapshotRepository, never()).getAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id());
        verify(snapshotRepository, never())
                .replaceAvailableSessionKeys(eq(PUPPET_THEATRE_TRACKED_LINK.id()), anySet());
        verifyNoInteractions(notificationService);
        verify(ticketproMetrics).recordFailure(TicketproCheckResult.ANTIBOT);
        verifyNoInteractions(healthchecksClient);
    }

    @Test
    void shouldReportStorageFailureWithoutConfirmingTicketpro() {
        // Arrange
        when(client.getAvailableSessions()).thenReturn(Mono.just(List.of(SESSION)));
        when(snapshotRepository.getAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id()))
                .thenThrow(new IllegalStateException("redis unavailable"));

        // Act & Assert
        assertThrows(CompletionException.class, () -> handler.handle(PUPPET_THEATRE_TRACKED_LINK)
                .join());
        verify(ticketproMetrics).recordFailure(TicketproCheckResult.STORAGE_ERROR);
        verifyNoInteractions(healthchecksClient);
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotCountOrConfirmRequestSkippedDuringBackoff() {
        // Arrange
        when(client.getAvailableSessions())
                .thenReturn(Mono.error(TicketproException.duringBackoff(TicketproCheckResult.RATE_LIMITED)));

        // Act & Assert
        assertThrows(CompletionException.class, () -> handler.handle(PUPPET_THEATRE_TRACKED_LINK)
                .join());
        verifyNoInteractions(ticketproMetrics, healthchecksClient, snapshotRepository, notificationService);
    }

    @Test
    void shouldKeepCompleteBotMessageWithinTelegramLimit() {
        // Arrange
        PuppetTheatreSession largeSession = new PuppetTheatreSession(
                "Спектакль " + "Я".repeat(3900),
                "12.09.2026",
                "11:00",
                "38–40 BYN",
                "https://www.ticketpro.by/bilety-v-teatr/large/");
        when(client.getAvailableSessions()).thenReturn(Mono.just(List.of(largeSession)));
        when(snapshotRepository.getAvailableSessionKeys(PUPPET_THEATRE_TRACKED_LINK.id()))
                .thenReturn(Optional.of(Set.of()));
        when(notificationService.sendUpdate(eq(PUPPET_THEATRE_TRACKED_LINK), anyString()))
                .thenReturn(Mono.empty());

        // Act
        handler.handle(PUPPET_THEATRE_TRACKED_LINK).join();

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService)
                .sendUpdate(eq(PUPPET_THEATRE_TRACKED_LINK), messageCaptor.capture());
        String completeBotMessage = "Новое обновление!\nURL: "
                + PUPPET_THEATRE_TRACKED_LINK.url()
                + "\n"
                + messageCaptor.getValue();
        assertTrue(completeBotMessage.length() <= 4096);
        assertTrue(messageCaptor.getValue().contains("И ещё новых сеансов: 1"));
    }

    @Test
    void shouldAcceptCanonicalPuppetTheatreAfishaUrl() {
        // Act & Assert
        assertTrue(PuppetTheatreUpdateHandler.isPuppetTheatreLink("https://puppet-minsk.by/afisha"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://puppet-minsk.by/afisha/",
                "https://www.puppet-minsk.by/afisha?date=2026-09-12",
                "http://puppet-minsk.by/afisha#schedule",
                "https://puppet-minsk.by.example.com/afisha",
                "https://puppet-minsk.by/afisha/archive",
                "https://www.ticketpro.by/bilety-v-teatr/kot-v-sapogah/"
            })
    void shouldRejectNonPuppetTheatreAfishaUrls(String url) {
        // Act & Assert
        assertFalse(PuppetTheatreUpdateHandler.isPuppetTheatreLink(url));
    }
}
