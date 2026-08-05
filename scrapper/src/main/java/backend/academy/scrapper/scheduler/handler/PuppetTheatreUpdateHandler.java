package backend.academy.scrapper.scheduler.handler;

import backend.academy.scrapper.client.puppettheatre.PuppetTheatreClient;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreSession;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class PuppetTheatreUpdateHandler {
    private static final String PUPPET_THEATRE_URL = "https://puppet-minsk.by/afisha";
    private final PuppetTheatreClient puppetTheatreClient;
    private final PuppetTheatreSnapshotRepository snapshotRepository;
    private final NotificationService notificationService;
    private final MeterRegistry meterRegistry;

    public static boolean isPuppetTheatreLink(String url) {
        return PUPPET_THEATRE_URL.equals(url);
    }

    public CompletableFuture<Void> handle(TrackedLink trackedLink) {
        Timer.Sample sample = Timer.start(meterRegistry);
        return puppetTheatreClient
                .getAvailableSessions()
                .flatMap(availableSessions -> processAvailableSessions(trackedLink, availableSessions))
                .doOnError(error -> log.atError()
                        .setMessage("Failed to check puppet theatre ticket availability")
                        .addKeyValue("url", trackedLink.url())
                        .setCause(error)
                        .log())
                .doFinally(signal -> sample.stop(meterRegistry.timer("custom_scrape_time", "type", "puppet_theatre")))
                .toFuture();
    }

    private Mono<Void> processAvailableSessions(TrackedLink trackedLink, List<PuppetTheatreSession> availableSessions) {
        Set<String> currentKeys = availableSessions.stream()
                .map(PuppetTheatreSession::snapshotKey)
                .collect(Collectors.toSet());
        Optional<Set<String>> previousKeys = snapshotRepository.getAvailableSessionKeys(trackedLink.id());

        if (previousKeys.isEmpty()) {
            return Mono.fromRunnable(
                    () -> snapshotRepository.replaceAvailableSessionKeys(trackedLink.id(), currentKeys));
        }

        List<PuppetTheatreSession> newSessions = availableSessions.stream()
                .filter(session -> !previousKeys.get().contains(session.snapshotKey()))
                .toList();
        Mono<Void> notification = newSessions.isEmpty()
                ? Mono.empty()
                : notificationService.sendUpdate(trackedLink, MessageFormatter.formatPuppetTheatreTickets(newSessions));

        return notification.then(
                Mono.fromRunnable(() -> snapshotRepository.replaceAvailableSessionKeys(trackedLink.id(), currentKeys)));
    }
}
