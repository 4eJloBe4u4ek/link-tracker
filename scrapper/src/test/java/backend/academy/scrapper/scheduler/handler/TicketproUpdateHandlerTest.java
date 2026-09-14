package backend.academy.scrapper.scheduler.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.ticketpro.TicketproClient;
import backend.academy.scrapper.client.ticketpro.TicketproEvent;
import backend.academy.scrapper.client.ticketpro.TicketproVenue;
import backend.academy.scrapper.repository.TicketproSnapshotRepository;
import backend.academy.scrapper.scheduler.service.NotificationService;
import backend.academy.shared.dto.TrackedLink;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
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
class TicketproUpdateHandlerTest {
    private static final int MAX_CANONICAL_TICKETPRO_VENUE_URL_LENGTH = 255;
    private static final String TICKETPRO_VENUE_URL_PREFIX = "https://www.ticketpro.by/koncertnye-ploshhadki/";
    private static final TrackedLink DVORETS_TRACKED_LINK = new TrackedLink(
            42L,
            "https://www.ticketpro.by/koncertnye-ploshhadki/dvorec-respubliki/",
            List.of(),
            List.of(),
            LocalDateTime.parse("2026-08-18T12:00:00"),
            LocalDateTime.parse("2026-08-18T12:00:00"));
    private static final TrackedLink KZ_MINSK_TRACKED_LINK = new TrackedLink(
            43L,
            "https://www.ticketpro.by/koncertnye-ploshhadki/kz-minsk/",
            List.of(),
            List.of(),
            LocalDateTime.parse("2026-08-18T12:00:00"),
            LocalDateTime.parse("2026-08-18T12:00:00"));
    private static final TicketproEvent EVENT = new TicketproEvent(
            "Кот в сапогах",
            "12.09.2026",
            "11:00",
            "38–40 BYN",
            "https://www.ticketpro.by/bilety-v-teatr/kot-v-sapogah/");
    private static final TicketproEvent DVORETS_EVENT = new TicketproEvent(
            "Граф Монте-Кристо",
            "18.08.2026",
            "19:00",
            "от 50 BYN",
            "https://www.ticketpro.by/bilety-v-teatr/graf-monte-kristo/");

    @Mock
    private TicketproClient client;

    @Mock
    private TicketproSnapshotRepository snapshotRepository;

    @Mock
    private NotificationService notificationService;

    private SimpleMeterRegistry meterRegistry;
    private TicketproUpdateHandler handler;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        handler = new TicketproUpdateHandler(client, snapshotRepository, notificationService, meterRegistry);
    }

    @Test
    void shouldInitializeSnapshotWithoutSendingOldTickets() {
        // Arrange
        when(client.getAvailableEvents(DVORETS_TRACKED_LINK.url()))
                .thenReturn(Mono.just(venue("ГУ Дворец Республики, Минск", List.of(DVORETS_EVENT))));
        when(snapshotRepository.getAvailableEventKeys(DVORETS_TRACKED_LINK.id()))
                .thenReturn(Optional.empty());

        // Act
        handler.handle(DVORETS_TRACKED_LINK).join();

        // Assert
        verify(client).getAvailableEvents(DVORETS_TRACKED_LINK.url());
        verify(notificationService, never()).sendUpdate(eq(DVORETS_TRACKED_LINK), anyString());
        verify(snapshotRepository)
                .replaceAvailableEventKeys(DVORETS_TRACKED_LINK.id(), Set.of(DVORETS_EVENT.snapshotKey()));
    }

    @Test
    void shouldNotifyWhenTicketsBecomeAvailable() {
        // Arrange
        Sinks.Empty<Void> pendingNotification = Sinks.empty();
        when(client.getAvailableEvents(DVORETS_TRACKED_LINK.url()))
                .thenReturn(Mono.just(venue("ГУ Дворец Республики, Минск", List.of(DVORETS_EVENT))));
        when(snapshotRepository.getAvailableEventKeys(DVORETS_TRACKED_LINK.id()))
                .thenReturn(Optional.of(Set.of()));
        when(notificationService.sendUpdate(eq(DVORETS_TRACKED_LINK), anyString()))
                .thenReturn(pendingNotification.asMono());

        // Act
        CompletableFuture<Void> result = handler.handle(DVORETS_TRACKED_LINK);

        // Assert
        assertFalse(result.isDone());
        verify(client).getAvailableEvents(DVORETS_TRACKED_LINK.url());
        verify(notificationService)
                .sendUpdate(
                        eq(DVORETS_TRACKED_LINK),
                        argThat(message -> message.contains("ГУ Дворец Республики, Минск")
                                && message.contains("Граф Монте-Кристо")));
        verify(snapshotRepository, never()).replaceAvailableEventKeys(eq(DVORETS_TRACKED_LINK.id()), any());

        // Act
        pendingNotification.tryEmitEmpty();
        result.join();

        // Assert
        verify(snapshotRepository)
                .replaceAvailableEventKeys(DVORETS_TRACKED_LINK.id(), Set.of(DVORETS_EVENT.snapshotKey()));
    }

    @Test
    void shouldNotifyOnlyNewEvents() {
        // Arrange
        when(client.getAvailableEvents(DVORETS_TRACKED_LINK.url()))
                .thenReturn(Mono.just(venue("ГУ Дворец Республики, Минск", List.of(EVENT, DVORETS_EVENT))));
        when(snapshotRepository.getAvailableEventKeys(DVORETS_TRACKED_LINK.id()))
                .thenReturn(Optional.of(Set.of(EVENT.snapshotKey())));
        when(notificationService.sendUpdate(eq(DVORETS_TRACKED_LINK), anyString()))
                .thenReturn(Mono.empty());

        // Act
        handler.handle(DVORETS_TRACKED_LINK).join();

        // Assert
        InOrder verificationOrder = inOrder(notificationService, snapshotRepository);
        verificationOrder
                .verify(notificationService)
                .sendUpdate(
                        eq(DVORETS_TRACKED_LINK),
                        argThat(message ->
                                message.contains(DVORETS_EVENT.title()) && !message.contains(EVENT.title())));
        verificationOrder
                .verify(snapshotRepository)
                .replaceAvailableEventKeys(
                        DVORETS_TRACKED_LINK.id(), Set.of(EVENT.snapshotKey(), DVORETS_EVENT.snapshotKey()));
    }

    @Test
    void shouldUseVenueNameFromEachTrackedTicketproPage() {
        // Arrange
        TicketproEvent concert = new TicketproEvent(
                "Концерт симфонической музыки",
                "20.08.2026",
                "19:00",
                "40–80 BYN",
                "https://www.ticketpro.by/bilety-na-koncert/simfonicheskaya-muzyka/");
        when(client.getAvailableEvents(DVORETS_TRACKED_LINK.url()))
                .thenReturn(Mono.just(venue("ГУ Дворец Республики, Минск", List.of(DVORETS_EVENT))));
        when(client.getAvailableEvents(KZ_MINSK_TRACKED_LINK.url()))
                .thenReturn(Mono.just(venue("Концертный зал Минск", List.of(concert))));
        when(snapshotRepository.getAvailableEventKeys(DVORETS_TRACKED_LINK.id()))
                .thenReturn(Optional.of(Set.of()));
        when(snapshotRepository.getAvailableEventKeys(KZ_MINSK_TRACKED_LINK.id()))
                .thenReturn(Optional.of(Set.of()));
        when(notificationService.sendUpdate(any(), anyString())).thenReturn(Mono.empty());

        // Act
        handler.handle(DVORETS_TRACKED_LINK).join();
        handler.handle(KZ_MINSK_TRACKED_LINK).join();

        // Assert
        verify(client).getAvailableEvents(DVORETS_TRACKED_LINK.url());
        verify(client).getAvailableEvents(KZ_MINSK_TRACKED_LINK.url());
        verify(notificationService)
                .sendUpdate(
                        eq(DVORETS_TRACKED_LINK),
                        argThat(message -> message.contains("Площадка: ГУ Дворец Республики, Минск")
                                && message.contains(DVORETS_EVENT.title())));
        verify(notificationService)
                .sendUpdate(
                        eq(KZ_MINSK_TRACKED_LINK),
                        argThat(message -> message.contains("Площадка: Концертный зал Минск")
                                && message.contains(concert.title())));
    }

    @Test
    void shouldKeepSnapshotAndNotNotifyWhenAvailableEventsDoNotChange() {
        // Arrange
        when(client.getAvailableEvents(DVORETS_TRACKED_LINK.url()))
                .thenReturn(Mono.just(venue("ГУ Дворец Республики, Минск", List.of(EVENT))));
        when(snapshotRepository.getAvailableEventKeys(DVORETS_TRACKED_LINK.id()))
                .thenReturn(Optional.of(Set.of(EVENT.snapshotKey())));

        // Act
        handler.handle(DVORETS_TRACKED_LINK).join();

        // Assert
        verify(notificationService, never()).sendUpdate(eq(DVORETS_TRACKED_LINK), anyString());
        verify(snapshotRepository)
                .replaceAvailableEventKeys(DVORETS_TRACKED_LINK.id(), Set.of(EVENT.snapshotKey()));
    }

    @Test
    void shouldKeepSnapshotWhenNotificationFails() {
        // Arrange
        when(client.getAvailableEvents(DVORETS_TRACKED_LINK.url()))
                .thenReturn(Mono.just(venue("ГУ Дворец Республики, Минск", List.of(EVENT))));
        when(snapshotRepository.getAvailableEventKeys(DVORETS_TRACKED_LINK.id()))
                .thenReturn(Optional.of(Set.of()));
        when(notificationService.sendUpdate(eq(DVORETS_TRACKED_LINK), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("delivery failed")));

        // Act
        CompletableFuture<Void> result = handler.handle(DVORETS_TRACKED_LINK);

        // Assert
        assertThrows(CompletionException.class, result::join);
        verify(snapshotRepository, never()).replaceAvailableEventKeys(eq(DVORETS_TRACKED_LINK.id()), any());
    }

    @Test
    void shouldNotifyWhenEventReappearsAfterBeingUnavailable() {
        // Arrange
        when(client.getAvailableEvents(DVORETS_TRACKED_LINK.url()))
                .thenReturn(
                        Mono.just(venue("ГУ Дворец Республики, Минск", List.of())),
                        Mono.just(venue("ГУ Дворец Республики, Минск", List.of(EVENT))));
        when(snapshotRepository.getAvailableEventKeys(DVORETS_TRACKED_LINK.id()))
                .thenReturn(Optional.of(Set.of(EVENT.snapshotKey())), Optional.of(Set.of()));
        when(notificationService.sendUpdate(eq(DVORETS_TRACKED_LINK), anyString()))
                .thenReturn(Mono.empty());

        // Act
        handler.handle(DVORETS_TRACKED_LINK).join();
        handler.handle(DVORETS_TRACKED_LINK).join();

        // Assert
        InOrder verificationOrder = inOrder(snapshotRepository, notificationService);
        verificationOrder.verify(snapshotRepository).replaceAvailableEventKeys(DVORETS_TRACKED_LINK.id(), Set.of());
        verificationOrder.verify(notificationService).sendUpdate(eq(DVORETS_TRACKED_LINK), contains(EVENT.title()));
        verificationOrder
                .verify(snapshotRepository)
                .replaceAvailableEventKeys(DVORETS_TRACKED_LINK.id(), Set.of(EVENT.snapshotKey()));
    }

    @Test
    void shouldKeepSnapshotWhenCatalogCheckFails() {
        // Arrange
        when(client.getAvailableEvents(DVORETS_TRACKED_LINK.url()))
                .thenReturn(Mono.error(new IllegalStateException("bot-protection")));

        // Act
        CompletableFuture<Void> result = handler.handle(DVORETS_TRACKED_LINK);

        // Assert
        assertThrows(CompletionException.class, result::join);
        verify(snapshotRepository, never()).getAvailableEventKeys(DVORETS_TRACKED_LINK.id());
        verify(snapshotRepository, never()).replaceAvailableEventKeys(eq(DVORETS_TRACKED_LINK.id()), any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldKeepCompleteBotMessageWithinTelegramLimit() {
        // Arrange
        TrackedLink longestUrlTrackedLink = new TrackedLink(
                DVORETS_TRACKED_LINK.id(),
                canonicalVenueUrl(MAX_CANONICAL_TICKETPRO_VENUE_URL_LENGTH),
                DVORETS_TRACKED_LINK.tags(),
                DVORETS_TRACKED_LINK.filters(),
                DVORETS_TRACKED_LINK.createdAt(),
                DVORETS_TRACKED_LINK.updatedAt());
        TicketproEvent largeEvent = new TicketproEvent(
                "Спектакль " + "Я".repeat(3900),
                "12.09.2026",
                "11:00",
                "38–40 BYN",
                "https://www.ticketpro.by/bilety-v-teatr/large/");
        when(client.getAvailableEvents(longestUrlTrackedLink.url()))
                .thenReturn(Mono.just(venue("ГУ Дворец Республики, Минск", List.of(largeEvent))));
        when(snapshotRepository.getAvailableEventKeys(longestUrlTrackedLink.id()))
                .thenReturn(Optional.of(Set.of()));
        when(notificationService.sendUpdate(eq(longestUrlTrackedLink), anyString()))
                .thenReturn(Mono.empty());

        // Act
        handler.handle(longestUrlTrackedLink).join();

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendUpdate(eq(longestUrlTrackedLink), messageCaptor.capture());
        String completeBotMessage =
                "Новое обновление!\nURL: " + longestUrlTrackedLink.url() + "\n" + messageCaptor.getValue();
        assertTrue(completeBotMessage.length() <= 4096);
        assertTrue(messageCaptor.getValue().contains("И ещё новых событий: 1"));
    }

    @Test
    void shouldRecordTicketproScrapeTimer() {
        // Arrange
        when(client.getAvailableEvents(DVORETS_TRACKED_LINK.url()))
                .thenReturn(Mono.just(venue("ГУ Дворец Республики, Минск", List.of())));
        when(snapshotRepository.getAvailableEventKeys(DVORETS_TRACKED_LINK.id()))
                .thenReturn(Optional.empty());

        // Act
        handler.handle(DVORETS_TRACKED_LINK).join();

        // Assert
        assertEquals(
                1L,
                meterRegistry.timer("custom_scrape_time", "type", "ticketpro").count());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://www.ticketpro.by/koncertnye-ploshhadki/belorusskij-gosudarstvennyj-teatr-kukol/",
                "https://www.ticketpro.by/koncertnye-ploshhadki/dvorec-respubliki/",
                "https://www.ticketpro.by/koncertnye-ploshhadki/kz-minsk/"
            })
    void shouldAcceptCanonicalTicketproVenueUrl(String url) {
        // Arrange

        // Act
        boolean result = TicketproUpdateHandler.isTicketproLink(url);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldAcceptCanonicalTicketproVenueUrlAtMaximumLength() {
        // Arrange
        String url = canonicalVenueUrl(MAX_CANONICAL_TICKETPRO_VENUE_URL_LENGTH);

        // Act
        boolean result = TicketproUpdateHandler.isTicketproLink(url);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldRejectCanonicalTicketproVenueUrlOverMaximumLength() {
        // Arrange
        String url = canonicalVenueUrl(MAX_CANONICAL_TICKETPRO_VENUE_URL_LENGTH + 1);

        // Act
        boolean result = TicketproUpdateHandler.isTicketproLink(url);

        // Assert
        assertFalse(result);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://www.ticketpro.by/koncertnye-ploshhadki/",
                "https://www.ticketpro.by/koncertnye-ploshhadki/dvorec-respubliki",
                "http://www.ticketpro.by/koncertnye-ploshhadki/dvorec-respubliki/",
                "https://ticketpro.by/koncertnye-ploshhadki/dvorec-respubliki/",
                "https://www.ticketpro.by/koncertnye-ploshhadki/dvorec-respubliki/?page=2",
                "https://www.ticketpro.by/koncertnye-ploshhadki/dvorec-respubliki/#events",
                "https://www.ticketpro.by/koncertnye-ploshhadki/a/b/",
                "https://www.ticketpro.by/bilety-v-teatr/graf-monte-kristo/",
                "https://www.ticketpro.by.evil/koncertnye-ploshhadki/dvorec-respubliki/",
                "https://puppet-minsk.by/afisha"
            })
    void shouldRejectNonTicketproVenueUrls(String url) {
        // Arrange

        // Act
        boolean result = TicketproUpdateHandler.isTicketproLink(url);

        // Assert
        assertFalse(result);
    }

    private TicketproVenue venue(String name, List<TicketproEvent> events) {
        return new TicketproVenue(name, events);
    }

    private static String canonicalVenueUrl(int length) {
        return TICKETPRO_VENUE_URL_PREFIX + "a".repeat(length - TICKETPRO_VENUE_URL_PREFIX.length() - 1) + "/";
    }
}
