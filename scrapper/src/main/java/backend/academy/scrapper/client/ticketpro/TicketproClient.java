package backend.academy.scrapper.client.ticketpro;

import backend.academy.scrapper.config.ScrapperConfig;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class TicketproClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final int PAGE_MAX_IN_MEMORY_SIZE = 2 * 1024 * 1024;

    private final TicketproEventParser eventParser;
    private final URI ticketproBaseUri;
    private final WebClient ticketproClient;

    public TicketproClient(ScrapperConfig scrapperConfig, TicketproEventParser eventParser) {
        this.eventParser = eventParser;
        this.ticketproBaseUri = URI.create(scrapperConfig.ticketpro().baseUrl());
        this.ticketproClient = WebClient.builder()
                .baseUrl(ticketproBaseUri.toString())
                .defaultHeader(HttpHeaders.ACCEPT, "text/html")
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(PAGE_MAX_IN_MEMORY_SIZE))
                        .build())
                .build();
    }

    public Mono<TicketproVenue> getAvailableEvents(String venueUrl) {
        return Mono.defer(() -> {
            URI venueUri = validateVenueUri(venueUrl);
            return getVenuePage(venueUri, 1).map(eventParser::parseVenuePage).flatMap(firstPage -> Flux.range(
                            2, Math.max(0, firstPage.lastPage() - 1))
                    .concatMap(page -> getVenuePage(venueUri, page))
                    .map(eventParser::parseVenuePage)
                    .startWith(firstPage)
                    .collectList()
                    .map(this::mergeVenuePages));
        });
    }

    private TicketproVenue mergeVenuePages(List<TicketproEventParser.VenuePage> pages) {
        String venueName = pages.getFirst().venueName();
        LinkedHashMap<String, TicketproEvent> events = new LinkedHashMap<>();
        for (TicketproEventParser.VenuePage page : pages) {
            if (!venueName.equals(page.venueName())) {
                throw new IllegalStateException("Ticketpro venue name differs between pages");
            }
            page.events().forEach(event -> events.putIfAbsent(event.snapshotKey(), event));
        }
        return new TicketproVenue(venueName, List.copyOf(events.values()));
    }

    private URI validateVenueUri(String venueUrl) {
        final URI venueUri;
        try {
            venueUri = URI.create(venueUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid Ticketpro venue URL", exception);
        }

        if (!hasExpectedOrigin(venueUri)) {
            throw new IllegalArgumentException("Ticketpro venue URL has an unexpected origin");
        }
        if (venueUri.getRawUserInfo() != null
                || venueUri.getRawQuery() != null
                || venueUri.getRawFragment() != null
                || venueUri.getRawPath() == null
                || venueUri.getRawPath().isBlank()
                || !venueUri.getRawPath().startsWith("/")) {
            throw new IllegalArgumentException("Ticketpro venue URL has unsafe URI components");
        }
        return venueUri;
    }

    private boolean hasExpectedOrigin(URI uri) {
        return uri.getScheme() != null
                && uri.getHost() != null
                && ticketproBaseUri.getScheme().equalsIgnoreCase(uri.getScheme())
                && ticketproBaseUri.getHost().equalsIgnoreCase(uri.getHost())
                && effectivePort(ticketproBaseUri) == effectivePort(uri);
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return switch (uri.getScheme().toLowerCase(Locale.ROOT)) {
            case "http" -> 80;
            case "https" -> 443;
            default -> -1;
        };
    }

    private Mono<String> getVenuePage(URI venueUri, int page) {
        return ticketproClient
                .get()
                .uri(createPageUri(venueUri, page))
                .retrieve()
                .toEntity(String.class)
                .flatMap(response -> {
                    MediaType contentType = response.getHeaders().getContentType();
                    if (contentType == null || !MediaType.TEXT_HTML.isCompatibleWith(contentType)) {
                        return Mono.error(new IllegalStateException("Ticketpro returned non-HTML content"));
                    }

                    String body = response.getBody();
                    if (body == null || body.isBlank()) {
                        return Mono.error(new IllegalStateException("Ticketpro returned empty HTML"));
                    }
                    return Mono.just(body);
                })
                .timeout(REQUEST_TIMEOUT);
    }

    private URI createPageUri(URI venueUri, int page) {
        if (page == 1) {
            return venueUri;
        }
        return UriComponentsBuilder.fromUri(venueUri)
                .queryParam("page", page)
                .build(true)
                .toUri();
    }
}
