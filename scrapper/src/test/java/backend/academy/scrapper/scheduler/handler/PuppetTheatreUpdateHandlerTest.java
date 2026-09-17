package backend.academy.scrapper.scheduler.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.puppettheatre.PuppetTheatreCheckResult;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreClient;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreException;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatrePage;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreSession;
import backend.academy.scrapper.monitoring.HealthchecksClient;
import backend.academy.scrapper.monitoring.PuppetTheatreMetrics;
import backend.academy.scrapper.repository.PuppetTheatreSnapshotRepository;
import backend.academy.scrapper.scheduler.service.NotificationService;
import backend.academy.shared.dto.TrackedLink;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PuppetTheatreUpdateHandlerTest {
    private static final TrackedLink TRACKED_LINK = new TrackedLink(
            42L,
            "https://puppet-minsk.by/afisha",
            List.of(),
            List.of(),
            LocalDateTime.parse("2026-09-14T12:00:00"),
            LocalDateTime.parse("2026-09-14T12:00:00"));
    private static final PuppetTheatreSession EXISTING = new PuppetTheatreSession(
            "Буратино",
            LocalDate.of(2026, 9, 20),
            LocalTime.of(11, 0),
            "https://puppet-minsk.by/spektakli/buratino#tickets");
    private static final PuppetTheatreSession NEW_SESSION = new PuppetTheatreSession(
            "Мойдодыр",
            LocalDate.of(2026, 10, 2),
            LocalTime.of(19, 30),
            "https://puppet-minsk.by/spektakli/mojdodyr#tickets");

    @Mock
    private PuppetTheatreClient client;

    @Mock
    private PuppetTheatreSnapshotRepository snapshotRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PuppetTheatreMetrics metrics;

    @Mock
    private HealthchecksClient healthchecksClient;

    private PuppetTheatreUpdateHandler handler;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        handler = new PuppetTheatreUpdateHandler(
                client, snapshotRepository, notificationService, meterRegistry, metrics, healthchecksClient);
    }

    @Test
    void shouldInitializeMissingSnapshotWithoutSendingExistingSessions() {
        when(client.getAvailableSessions()).thenReturn(Mono.just(page(EXISTING)));
        when(snapshotRepository.getAvailableSessionKeys(42L)).thenReturn(Optional.empty());

        handler.handle(TRACKED_LINK).join();

        verify(notificationService, never()).sendUpdate(eq(TRACKED_LINK), anyString());
        verify(snapshotRepository).replaceAvailableSessionKeys(42L, Set.of(EXISTING.snapshotKey()));
        verify(metrics).recordSuccess(1, 2);
        verify(healthchecksClient).pingPuppetTheatre();
    }

    @Test
    void shouldNotifyWhenSessionsAppearAfterEmptySnapshot() {
        when(client.getAvailableSessions()).thenReturn(Mono.just(page(EXISTING)));
        when(snapshotRepository.getAvailableSessionKeys(42L)).thenReturn(Optional.of(Set.of()));
        when(notificationService.sendUpdate(eq(TRACKED_LINK), anyString())).thenReturn(Mono.empty());

        handler.handle(TRACKED_LINK).join();

        verify(notificationService).sendUpdate(
                eq(TRACKED_LINK), org.mockito.ArgumentMatchers.argThat(message -> message.contains("Буратино")));
        verify(snapshotRepository).replaceAvailableSessionKeys(42L, Set.of(EXISTING.snapshotKey()));
    }

    @Test
    void shouldNotifyOnlyNewSessionsThenReplaceSnapshot() {
        when(client.getAvailableSessions()).thenReturn(Mono.just(page(EXISTING, NEW_SESSION)));
        when(snapshotRepository.getAvailableSessionKeys(42L)).thenReturn(Optional.of(Set.of(EXISTING.snapshotKey())));
        when(notificationService.sendUpdate(eq(TRACKED_LINK), anyString())).thenReturn(Mono.empty());

        handler.handle(TRACKED_LINK).join();

        InOrder order = inOrder(notificationService, snapshotRepository);
        order.verify(notificationService)
                .sendUpdate(
                        eq(TRACKED_LINK),
                        org.mockito.ArgumentMatchers.argThat(message -> message.contains("Мойдодыр")
                                && !message.contains("Буратино")
                                && message.contains("02.10.2026")
                                && message.contains("19:30")));
        order.verify(snapshotRepository)
                .replaceAvailableSessionKeys(42L, Set.of(EXISTING.snapshotKey(), NEW_SESSION.snapshotKey()));
    }

    @Test
    void shouldNotifyWhenAPreviouslyDisappearedSessionReappears() {
        when(client.getAvailableSessions())
                .thenReturn(Mono.just(page(NEW_SESSION)), Mono.just(page(EXISTING, NEW_SESSION)));
        when(snapshotRepository.getAvailableSessionKeys(42L))
                .thenReturn(
                        Optional.of(Set.of(EXISTING.snapshotKey(), NEW_SESSION.snapshotKey())),
                        Optional.of(Set.of(NEW_SESSION.snapshotKey())));
        when(notificationService.sendUpdate(eq(TRACKED_LINK), anyString())).thenReturn(Mono.empty());

        handler.handle(TRACKED_LINK).join();
        handler.handle(TRACKED_LINK).join();

        verify(notificationService)
                .sendUpdate(
                        eq(TRACKED_LINK),
                        org.mockito.ArgumentMatchers.argThat(
                                message -> message.contains("Буратино") && !message.contains("Мойдодыр")));
        InOrder order = inOrder(snapshotRepository);
        order.verify(snapshotRepository).replaceAvailableSessionKeys(42L, Set.of(NEW_SESSION.snapshotKey()));
        order.verify(snapshotRepository)
                .replaceAvailableSessionKeys(42L, Set.of(EXISTING.snapshotKey(), NEW_SESSION.snapshotKey()));
    }

    @Test
    void shouldLeaveSnapshotUnchangedWhenNotificationFails() {
        when(client.getAvailableSessions()).thenReturn(Mono.just(page(EXISTING, NEW_SESSION)));
        when(snapshotRepository.getAvailableSessionKeys(42L)).thenReturn(Optional.of(Set.of(EXISTING.snapshotKey())));
        when(notificationService.sendUpdate(eq(TRACKED_LINK), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("delivery failed")));

        assertThrows(
                CompletionException.class, () -> handler.handle(TRACKED_LINK).join());

        verify(snapshotRepository, never()).replaceAvailableSessionKeys(eq(42L), org.mockito.ArgumentMatchers.anySet());
        verify(metrics).recordFailure(PuppetTheatreCheckResult.DELIVERY_ERROR);
        verifyNoInteractions(healthchecksClient);
    }

    @Test
    void shouldLeaveSnapshotUnchangedWhenSnapshotReadFails() {
        when(client.getAvailableSessions()).thenReturn(Mono.just(page(EXISTING)));
        when(snapshotRepository.getAvailableSessionKeys(42L)).thenThrow(new IllegalStateException("read failed"));

        assertThrows(
                CompletionException.class, () -> handler.handle(TRACKED_LINK).join());

        verify(snapshotRepository, never()).replaceAvailableSessionKeys(eq(42L), org.mockito.ArgumentMatchers.anySet());
        verifyNoInteractions(notificationService);
        verify(metrics).recordFailure(PuppetTheatreCheckResult.STORAGE_ERROR);
        verifyNoInteractions(healthchecksClient);
    }

    @Test
    void shouldReportSnapshotWriteFailureWithoutClaimingSuccess() {
        when(client.getAvailableSessions()).thenReturn(Mono.just(page(EXISTING)));
        when(snapshotRepository.getAvailableSessionKeys(42L)).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("write failed"))
                .when(snapshotRepository)
                .replaceAvailableSessionKeys(42L, Set.of(EXISTING.snapshotKey()));

        assertThrows(
                CompletionException.class, () -> handler.handle(TRACKED_LINK).join());

        verifyNoInteractions(notificationService);
        verify(metrics).recordFailure(PuppetTheatreCheckResult.STORAGE_ERROR);
        verify(metrics, never()).recordSuccess(1, 2);
        verifyNoInteractions(healthchecksClient);
    }

    @Test
    void shouldRecordScrapeTimeWithCanonicalPuppetTheatreType() {
        when(client.getAvailableSessions()).thenReturn(Mono.just(page(EXISTING)));
        when(snapshotRepository.getAvailableSessionKeys(42L)).thenReturn(Optional.empty());

        handler.handle(TRACKED_LINK).join();

        assertThat(meterRegistry
                        .find("custom_scrape_time")
                        .tag("type", "puppet_theatre")
                        .timer())
                .isNotNull();
        assertThat(meterRegistry
                        .find("custom_scrape_time")
                        .tag("type", "puppet-theatre")
                        .timer())
                .isNull();
    }

    @Test
    void shouldPreserveSnapshotAndReportFailureWhenPageCheckFails() {
        when(client.getAvailableSessions())
                .thenReturn(Mono.error(new PuppetTheatreException(PuppetTheatreCheckResult.ANTIBOT, "challenge")));

        assertThrows(
                CompletionException.class, () -> handler.handle(TRACKED_LINK).join());

        verifyNoInteractions(snapshotRepository, notificationService);
        verify(metrics).recordFailure(PuppetTheatreCheckResult.ANTIBOT);
        verifyNoInteractions(healthchecksClient);
    }

    @Test
    void shouldNotReportSchedulerChecksSkippedDuringBackoff() {
        when(client.getAvailableSessions())
                .thenReturn(
                        Mono.error(new PuppetTheatreException(PuppetTheatreCheckResult.SKIPPED_BACKOFF, "backoff")));

        assertThrows(
                CompletionException.class, () -> handler.handle(TRACKED_LINK).join());

        verifyNoInteractions(metrics, healthchecksClient, snapshotRepository, notificationService);
    }

    @Test
    void shouldRecognizeOnlyTheExactDirectAfishaUrl() {
        assertThat(PuppetTheatreUpdateHandler.isPuppetTheatreLink("https://puppet-minsk.by/afisha"))
                .isTrue();
        assertThat(PuppetTheatreUpdateHandler.isPuppetTheatreLink("https://puppet-minsk.by/afisha/"))
                .isFalse();
        assertThat(PuppetTheatreUpdateHandler.isPuppetTheatreLink("https://puppet-minsk.by/afisha?month=2026-09#top"))
                .isFalse();
        assertThat(PuppetTheatreUpdateHandler.isPuppetTheatreLink("http://puppet-minsk.by/afisha"))
                .isFalse();
        assertThat(PuppetTheatreUpdateHandler.isPuppetTheatreLink("https://puppet-minsk.by/spektakli"))
                .isFalse();
        assertThat(PuppetTheatreUpdateHandler.isPuppetTheatreLink("https://puppet-minsk.by.evil/afisha"))
                .isFalse();
    }

    private PuppetTheatrePage page(PuppetTheatreSession... sessions) {
        return new PuppetTheatrePage(List.of("2026-09", "2026-10"), List.of(sessions));
    }
}
