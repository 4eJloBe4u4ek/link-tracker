package backend.academy.scrapper.scheduler.handler;

import backend.academy.scrapper.client.ticketpro.TicketproClient;
import backend.academy.scrapper.client.ticketpro.TicketproCheckResult;
import backend.academy.scrapper.client.ticketpro.TicketproEvent;
import backend.academy.scrapper.client.ticketpro.TicketproException;
import backend.academy.scrapper.client.ticketpro.TicketproVenue;
import backend.academy.scrapper.monitoring.HealthchecksClient;
import backend.academy.scrapper.monitoring.TicketproMetrics;
import backend.academy.scrapper.repository.TicketproSnapshotRepository;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketproUpdateHandler {
    // Bounds the canonical URL stored for notifications while leaving room for the Ticketpro description.
    private static final int MAX_TICKETPRO_VENUE_URL_LENGTH = 255;
    private static final Pattern TICKETPRO_VENUE_LINK_PATTERN =
            Pattern.compile("^https://www\\.ticketpro\\.by/koncertnye-ploshhadki/[a-z0-9]+(?:-[a-z0-9]+)*/$");
    private final TicketproClient ticketproClient;
    private final TicketproSnapshotRepository snapshotRepository;
    private final NotificationService notificationService;
    private final MeterRegistry meterRegistry;
    private final TicketproMetrics ticketproMetrics;
    private final HealthchecksClient healthchecksClient;
    private final Set<Long> checksInProgress = ConcurrentHashMap.newKeySet();

    public static boolean isTicketproLink(String url) {
        return url != null
                && url.length() <= MAX_TICKETPRO_VENUE_URL_LENGTH
                && TICKETPRO_VENUE_LINK_PATTERN.matcher(url).matches();
    }

    public CompletableFuture<Void> handle(TrackedLink trackedLink) {
        if (!checksInProgress.add(trackedLink.id())) {
            return CompletableFuture.completedFuture(null);
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return ticketproClient
                    .getAvailableEvents(trackedLink.url())
                    .flatMap(venue -> processAvailableEvents(trackedLink, venue).thenReturn(venue.events().size()))
                    .doOnSuccess(this::recordSuccess)
                    .doOnError(error -> {
                        log.atError()
                                .setMessage("Failed to check Ticketpro venue ticket availability")
                                .addKeyValue("url", trackedLink.url())
                                .setCause(error)
                                .log();
                        recordFailure(error);
                    })
                    .doFinally(
                            signal -> sample.stop(meterRegistry.timer("custom_scrape_time", "type", "ticketpro")))
                    .then()
                    .toFuture()
                    .whenComplete((ignored, error) -> checksInProgress.remove(trackedLink.id()));
        } catch (RuntimeException exception) {
            checksInProgress.remove(trackedLink.id());
            throw exception;
        }
    }

    private Mono<Void> processAvailableEvents(TrackedLink trackedLink, TicketproVenue venue) {
        List<TicketproEvent> availableEvents = venue.events();
        Set<String> currentKeys =
                availableEvents.stream().map(TicketproEvent::snapshotKey).collect(Collectors.toSet());
        return loadSnapshot(trackedLink.id()).flatMap(previousKeys -> {
            if (previousKeys.isEmpty()) {
                return replaceSnapshot(trackedLink.id(), currentKeys);
            }

            List<TicketproEvent> newEvents = availableEvents.stream()
                    .filter(event -> !previousKeys.orElseThrow().contains(event.snapshotKey()))
                    .toList();
            return sendNotification(trackedLink, venue.name(), newEvents)
                    .then(replaceSnapshot(trackedLink.id(), currentKeys));
        });
    }

    private Mono<Optional<Set<String>>> loadSnapshot(long linkId) {
        return Mono.fromCallable(() -> snapshotRepository.getAvailableEventKeys(linkId))
                .onErrorMap(error -> new TicketproException(
                        TicketproCheckResult.STORAGE_ERROR, "Failed to read Ticketpro snapshot", error));
    }

    private Mono<Void> replaceSnapshot(long linkId, Set<String> currentKeys) {
        return Mono.<Void>fromRunnable(() -> snapshotRepository.replaceAvailableEventKeys(linkId, currentKeys))
                .onErrorMap(error -> new TicketproException(
                        TicketproCheckResult.STORAGE_ERROR, "Failed to update Ticketpro snapshot", error));
    }

    private Mono<Void> sendNotification(TrackedLink trackedLink, String venueName, List<TicketproEvent> newEvents) {
        if (newEvents.isEmpty()) {
            return Mono.empty();
        }
        return Mono.defer(() -> notificationService.sendUpdate(
                        trackedLink, MessageFormatter.formatTicketproEvents(venueName, newEvents)))
                .onErrorMap(error -> new TicketproException(
                        TicketproCheckResult.DELIVERY_ERROR, "Failed to deliver Ticketpro notification", error));
    }

    private void recordSuccess(int availableEventCount) {
        ticketproMetrics.recordSuccess(availableEventCount);
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
