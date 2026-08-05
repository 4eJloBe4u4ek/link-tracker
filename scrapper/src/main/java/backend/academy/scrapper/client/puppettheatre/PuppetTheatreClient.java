package backend.academy.scrapper.client.puppettheatre;

import backend.academy.scrapper.config.ScrapperConfig;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class PuppetTheatreClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final int PAGE_MAX_IN_MEMORY_SIZE = 2 * 1024 * 1024;

    private final ScrapperConfig.PuppetTheatre properties;
    private final TicketproEventParser eventParser;
    private final WebClient ticketproClient;

    public PuppetTheatreClient(ScrapperConfig scrapperConfig, TicketproEventParser eventParser) {
        this.properties = scrapperConfig.puppetTheatre();
        this.eventParser = eventParser;
        this.ticketproClient = WebClient.builder()
                .baseUrl(properties.ticketproBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "text/html")
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(PAGE_MAX_IN_MEMORY_SIZE))
                        .build())
                .build();
    }

    public Mono<List<PuppetTheatreSession>> getAvailableSessions() {
        return getVenuePage(1).map(eventParser::parseVenuePage).flatMap(firstPage -> Flux.range(2, firstPage.lastPage() - 1)
                .concatMap(this::getVenuePage)
                .map(eventParser::parseVenuePage)
                .map(TicketproEventParser.VenuePage::sessions)
                .startWith(firstPage.sessions())
                .flatMapIterable(sessions -> sessions)
                .collect(
                        LinkedHashMap<String, PuppetTheatreSession>::new,
                        (sessions, session) -> sessions.putIfAbsent(session.snapshotKey(), session))
                .map(sessions -> List.copyOf(sessions.values())));
    }

    private Mono<String> getVenuePage(int page) {
        return ticketproClient
                .get()
                .uri(uriBuilder -> {
                    uriBuilder.path(properties.venuePath());
                    if (page > 1) {
                        uriBuilder.queryParam("page", page);
                    }
                    return uriBuilder.build();
                })
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
}
