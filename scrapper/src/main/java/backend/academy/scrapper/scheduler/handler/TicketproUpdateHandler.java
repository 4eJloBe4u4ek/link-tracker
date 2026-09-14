package backend.academy.scrapper.scheduler.handler;

import backend.academy.scrapper.client.ticketpro.TicketproClient;
import backend.academy.scrapper.client.ticketpro.TicketproEvent;
import backend.academy.scrapper.client.ticketpro.TicketproVenue;
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

    public static boolean isTicketproLink(String url) {
        return url != null
                && url.length() <= MAX_TICKETPRO_VENUE_URL_LENGTH
                && TICKETPRO_VENUE_LINK_PATTERN.matcher(url).matches();
    }

    public CompletableFuture<Void> handle(TrackedLink trackedLink) {
        Timer.Sample sample = Timer.start(meterRegistry);
        return ticketproClient
                .getAvailableEvents(trackedLink.url())
                .flatMap(venue -> processAvailableEvents(trackedLink, venue))
                .doOnError(error -> log.atError()
                        .setMessage("Failed to check Ticketpro venue ticket availability")
                        .addKeyValue("url", trackedLink.url())
                        .setCause(error)
                        .log())
                .doFinally(signal -> sample.stop(meterRegistry.timer("custom_scrape_time", "type", "ticketpro")))
                .toFuture();
    }

    private Mono<Void> processAvailableEvents(TrackedLink trackedLink, TicketproVenue venue) {
        List<TicketproEvent> availableEvents = venue.events();
        Set<String> currentKeys =
                availableEvents.stream().map(TicketproEvent::snapshotKey).collect(Collectors.toSet());
        Optional<Set<String>> previousKeys = snapshotRepository.getAvailableEventKeys(trackedLink.id());

        if (previousKeys.isEmpty()) {
            return Mono.fromRunnable(
                    () -> snapshotRepository.replaceAvailableEventKeys(trackedLink.id(), currentKeys));
        }

        List<TicketproEvent> newEvents = availableEvents.stream()
                .filter(event -> !previousKeys.get().contains(event.snapshotKey()))
                .toList();
        Mono<Void> notification = newEvents.isEmpty()
                ? Mono.empty()
                : notificationService.sendUpdate(
                        trackedLink, MessageFormatter.formatTicketproEvents(venue.name(), newEvents));

        return notification.then(
                Mono.fromRunnable(() -> snapshotRepository.replaceAvailableEventKeys(trackedLink.id(), currentKeys)));
    }
}
