package backend.academy.scrapper.scheduler.handler;

import backend.academy.scrapper.client.puppettheatre.PuppetTheatreCheckResult;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreClient;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreException;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatrePage;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreSession;
import backend.academy.scrapper.monitoring.HealthchecksClient;
import backend.academy.scrapper.monitoring.PuppetTheatreMetrics;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class PuppetTheatreUpdateHandler {
    private final PuppetTheatreClient client;
    private final PuppetTheatreSnapshotRepository snapshotRepository;
    private final NotificationService notificationService;
    private final MeterRegistry meterRegistry;
    private final PuppetTheatreMetrics metrics;
    private final HealthchecksClient healthchecksClient;
    private final Set<Long> checksInProgress = ConcurrentHashMap.newKeySet();

    public static boolean isPuppetTheatreLink(String url) {
        return PuppetTheatreClient.AFISHA_URI.toString().equals(url);
    }

    public CompletableFuture<Void> handle(TrackedLink trackedLink) {
        if (!checksInProgress.add(trackedLink.id())) {
            return CompletableFuture.completedFuture(null);
        }
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return client.getAvailableSessions()
                    .flatMap(page -> processAvailableSessions(trackedLink, page).thenReturn(page))
                    .doOnSuccess(page ->
                            recordSuccess(page.sessions().size(), page.months().size()))
                    .doOnError(this::recordFailure)
                    .doFinally(signal -> sample.stop(
                            meterRegistry.timer("custom_scrape_time", "type", PuppetTheatreMetrics.SOURCE_TYPE)))
                    .then()
                    .toFuture()
                    .whenComplete((ignored, error) -> checksInProgress.remove(trackedLink.id()));
        } catch (RuntimeException exception) {
            checksInProgress.remove(trackedLink.id());
            throw exception;
        }
    }

    private Mono<Void> processAvailableSessions(TrackedLink trackedLink, PuppetTheatrePage page) {
        Set<String> currentKeys =
                page.sessions().stream().map(PuppetTheatreSession::snapshotKey).collect(Collectors.toSet());
        return loadSnapshot(trackedLink.id()).flatMap(previousKeys -> {
            if (previousKeys.isEmpty()) {
                return replaceSnapshot(trackedLink.id(), currentKeys);
            }
            Set<String> previous = previousKeys.orElseThrow();
            List<PuppetTheatreSession> newSessions = page.sessions().stream()
                    .filter(session -> !previous.contains(session.snapshotKey()))
                    .toList();
            return sendNotification(trackedLink, newSessions).then(replaceSnapshot(trackedLink.id(), currentKeys));
        });
    }

    private Mono<Optional<Set<String>>> loadSnapshot(long linkId) {
        return Mono.fromCallable(() -> snapshotRepository.getAvailableSessionKeys(linkId))
                .onErrorMap(error -> new PuppetTheatreException(
                        PuppetTheatreCheckResult.STORAGE_ERROR, "Failed to read puppet theatre snapshot", error));
    }

    private Mono<Void> replaceSnapshot(long linkId, Set<String> currentKeys) {
        return Mono.<Void>fromRunnable(() -> snapshotRepository.replaceAvailableSessionKeys(linkId, currentKeys))
                .onErrorMap(error -> new PuppetTheatreException(
                        PuppetTheatreCheckResult.STORAGE_ERROR, "Failed to update puppet theatre snapshot", error));
    }

    private Mono<Void> sendNotification(TrackedLink trackedLink, List<PuppetTheatreSession> newSessions) {
        if (newSessions.isEmpty()) {
            return Mono.empty();
        }
        return Mono.defer(() -> notificationService.sendUpdate(
                        trackedLink, MessageFormatter.formatPuppetTheatreSessions(newSessions)))
                .onErrorMap(error -> new PuppetTheatreException(
                        PuppetTheatreCheckResult.DELIVERY_ERROR,
                        "Failed to deliver puppet theatre notification",
                        error));
    }

    private void recordSuccess(int sessionCount, int monthCount) {
        metrics.recordSuccess(sessionCount, monthCount);
        healthchecksClient.pingPuppetTheatre();
    }

    private void recordFailure(Throwable error) {
        PuppetTheatreCheckResult result = error instanceof PuppetTheatreException exception
                ? exception.result()
                : PuppetTheatreCheckResult.NETWORK_ERROR;
        if (result == PuppetTheatreCheckResult.SKIPPED_BACKOFF) {
            return;
        }
        log.atError()
                .setMessage("Failed to check puppet theatre afisha")
                .setCause(error)
                .log();
        metrics.recordFailure(result);
    }
}
