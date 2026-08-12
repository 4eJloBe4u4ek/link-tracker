package backend.academy.scrapper.scheduler.handler;

import backend.academy.scrapper.client.puppettheatre.PuppetTheatreClient;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreSession;
import backend.academy.scrapper.client.puppettheatre.TicketproCheckResult;
import backend.academy.scrapper.client.puppettheatre.TicketproException;
import backend.academy.scrapper.monitoring.HealthchecksClient;
import backend.academy.scrapper.monitoring.TicketproMetrics;
import backend.academy.scrapper.repository.PuppetTheatreSnapshotRepository;
import backend.academy.scrapper.scheduler.service.NotificationService;
import backend.academy.scrapper.scheduler.util.MessageFormatter;
import backend.academy.shared.dto.TrackedLink;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PuppetTheatreUpdateHandler {
    private static final String PUPPET_THEATRE_URL = "https://puppet-minsk.by/afisha";
    private final PuppetTheatreClient puppetTheatreClient;
    private final PuppetTheatreSnapshotRepository snapshotRepository;
    private final NotificationService notificationService;
    private final MeterRegistry meterRegistry;
    private final TicketproMetrics ticketproMetrics;
    private final HealthchecksClient healthchecksClient;
    private final AtomicBoolean checkInProgress = new AtomicBoolean();

    public static boolean isPuppetTheatreLink(String url) {
        return PUPPET_THEATRE_URL.equals(url);
    }

    public CompletableFuture<Void> handle(TrackedLink trackedLink) {
        if (!checkInProgress.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return puppetTheatreClient
                    .getAvailableSessions()
                    .flatMap(availableSessions -> processAvailableSessions(trackedLink, availableSessions)
                            .thenReturn(availableSessions.size()))
                    .doOnSuccess(this::recordSuccess)
                    .doOnError(this::recordFailure)
                    .doFinally(
                            signal -> sample.stop(meterRegistry.timer("custom_scrape_time", "type", "puppet_theatre")))
                    .then()
                    .toFuture()
                    .whenComplete((ignored, error) -> checkInProgress.set(false));
        } catch (RuntimeException exception) {
            checkInProgress.set(false);
            throw exception;
        }
    }

    private Mono<Void> processAvailableSessions(TrackedLink trackedLink, List<PuppetTheatreSession> availableSessions) {
        Set<String> currentKeys = availableSessions.stream()
                .map(PuppetTheatreSession::snapshotKey)
                .collect(Collectors.toSet());
        return loadSnapshot(trackedLink.id()).flatMap(previousKeys -> {
            if (previousKeys.isEmpty()) {
                return replaceSnapshot(trackedLink.id(), currentKeys);
            }

            List<PuppetTheatreSession> newSessions = availableSessions.stream()
                    .filter(session -> !previousKeys.orElseThrow().contains(session.snapshotKey()))
                    .toList();
            return sendNotification(trackedLink, newSessions)
                    .then(replaceSnapshot(trackedLink.id(), currentKeys));
        });
    }

    private Mono<Optional<Set<String>>> loadSnapshot(long linkId) {
        return Mono.fromCallable(() -> snapshotRepository.getAvailableSessionKeys(linkId))
                .onErrorMap(error -> new TicketproException(
                        TicketproCheckResult.STORAGE_ERROR, "Failed to read Ticketpro snapshot", error));
    }

    private Mono<Void> replaceSnapshot(long linkId, Set<String> currentKeys) {
        return Mono.<Void>fromRunnable(() -> snapshotRepository.replaceAvailableSessionKeys(linkId, currentKeys))
                .onErrorMap(error -> new TicketproException(
                        TicketproCheckResult.STORAGE_ERROR, "Failed to update Ticketpro snapshot", error));
    }

    private Mono<Void> sendNotification(TrackedLink trackedLink, List<PuppetTheatreSession> newSessions) {
        if (newSessions.isEmpty()) {
            return Mono.empty();
        }
        return Mono.defer(() -> notificationService.sendUpdate(
                        trackedLink, MessageFormatter.formatPuppetTheatreTickets(newSessions)))
                .onErrorMap(error -> new TicketproException(
                        TicketproCheckResult.DELIVERY_ERROR, "Failed to deliver Ticketpro notification", error));
    }

    private void recordSuccess(int availableSessionCount) {
        ticketproMetrics.recordSuccess(availableSessionCount);
        healthchecksClient.pingTicketpro();
    }

    private void recordFailure(Throwable error) {
        if (error instanceof TicketproException ticketproException && !ticketproException.reportable()) {
            return;
        }
        TicketproCheckResult result = error instanceof TicketproException ticketproException
                ? ticketproException.result()
                : TicketproCheckResult.NETWORK_ERROR;
        ticketproMetrics.recordFailure(result);
    }

}
