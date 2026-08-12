package backend.academy.scrapper.client.puppettheatre;

import backend.academy.scrapper.config.ScrapperConfig;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class PuppetTheatreClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration DEFAULT_RATE_LIMIT_BACKOFF = Duration.ofMinutes(5);
    private static final Duration FORBIDDEN_BACKOFF = Duration.ofMinutes(15);
    private static final Duration ANTI_BOT_BACKOFF = Duration.ofMinutes(15);
    private static final Duration MAXIMUM_BACKOFF = Duration.ofHours(1);
    private static final int PAGE_MAX_IN_MEMORY_SIZE = 2 * 1024 * 1024;

    private final ScrapperConfig.PuppetTheatre properties;
    private final TicketproEventParser eventParser;
    private final WebClient ticketproClient;
    private final Clock clock;
    private final Duration requestTimeout;
    private final TicketproBackoff backoff;
    private final AtomicReference<TicketproCheckResult> backoffReason = new AtomicReference<>();

    @Autowired
    public PuppetTheatreClient(ScrapperConfig scrapperConfig, TicketproEventParser eventParser) {
        this(scrapperConfig, eventParser, Clock.systemUTC(), REQUEST_TIMEOUT);
    }

    PuppetTheatreClient(
            ScrapperConfig scrapperConfig,
            TicketproEventParser eventParser,
            Clock clock,
            Duration requestTimeout) {
        this.properties = scrapperConfig.puppetTheatre();
        this.eventParser = eventParser;
        this.clock = clock;
        this.requestTimeout = requestTimeout;
        this.backoff = new TicketproBackoff(clock);
        this.ticketproClient = WebClient.builder()
                .baseUrl(properties.ticketproBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "text/html")
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(PAGE_MAX_IN_MEMORY_SIZE))
                        .build())
                .build();
    }

    public Mono<List<PuppetTheatreSession>> getAvailableSessions() {
        if (!backoff.requestAllowed()) {
            return Mono.error(TicketproException.duringBackoff(backoffReason.get()));
        }

        return getVenuePage(1)
                .map(eventParser::parseVenuePage)
                            .flatMap(firstPage -> Flux.range(2, firstPage.lastPage() - 1)
                                .concatMap(this::getVenuePage)
                                .map(eventParser::parseVenuePage)
                        .map(TicketproEventParser.VenuePage::sessions)
                        .startWith(firstPage.sessions())
                        .flatMapIterable(sessions -> sessions)
                        .collect(
                                LinkedHashMap<String, PuppetTheatreSession>::new,
                                (sessions, session) -> sessions.putIfAbsent(session.snapshotKey(), session))
                        .map(sessions -> List.copyOf(sessions.values())))
                .onErrorMap(this::classifyFailure)
                .doOnError(this::activateBackoffIfRequired)
                .doOnSuccess(ignored -> {
                    backoff.clear();
                    backoffReason.set(null);
                });
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
                .exchangeToMono(this::handleResponse)
                .timeout(requestTimeout);
    }

    private Mono<String> handleResponse(ClientResponse response) {
        HttpStatusCode status = response.statusCode();
        if (status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
            Duration retryAfter = parseRetryAfter(
                    response.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER));
            return failWithBackoff(
                    retryAfter, TicketproCheckResult.RATE_LIMITED, "Ticketpro rate limit exceeded");
        }
        if (status.isSameCodeAs(HttpStatus.FORBIDDEN)) {
            return failWithBackoff(
                    FORBIDDEN_BACKOFF, TicketproCheckResult.FORBIDDEN, "Ticketpro request forbidden");
        }
        if (!status.is2xxSuccessful()) {
            TicketproCheckResult result = status.is5xxServerError()
                    ? TicketproCheckResult.NETWORK_ERROR
                    : TicketproCheckResult.INVALID_CONTENT;
            return fail(result, "Ticketpro returned HTTP " + status.value());
        }
        return readHtml(response);
    }

    private Mono<String> readHtml(ClientResponse response) {
        MediaType contentType = response.headers().contentType().orElse(null);
        if (contentType == null || !MediaType.TEXT_HTML.isCompatibleWith(contentType)) {
            return fail(TicketproCheckResult.INVALID_CONTENT, "Ticketpro returned non-HTML content");
        }
        return response.bodyToMono(String.class)
                .filter(body -> !body.isBlank())
                .switchIfEmpty(fail(TicketproCheckResult.INVALID_CONTENT, "Ticketpro returned empty HTML"));
    }

    private Mono<String> failWithBackoff(
            Duration duration, TicketproCheckResult result, String message) {
        activateBackoff(duration, result);
        return fail(result, message);
    }

    private Mono<String> fail(TicketproCheckResult result, String message) {
        return Mono.error(new TicketproException(result, message));
    }

    private Throwable classifyFailure(Throwable error) {
        if (error instanceof TicketproException) {
            return error;
        }
        if (error instanceof TimeoutException) {
            return new TicketproException(TicketproCheckResult.TIMEOUT, "Ticketpro request timed out", error);
        }
        if (error instanceof DataBufferLimitException) {
            return new TicketproException(
                    TicketproCheckResult.INVALID_CONTENT, "Ticketpro response is too large", error);
        }
        if (error instanceof WebClientRequestException) {
            return new TicketproException(TicketproCheckResult.NETWORK_ERROR, "Ticketpro network request failed", error);
        }
        return new TicketproException(TicketproCheckResult.NETWORK_ERROR, "Ticketpro request failed", error);
    }

    private void activateBackoffIfRequired(Throwable error) {
        if (!(error instanceof TicketproException ticketproException) || !ticketproException.reportable()) {
            return;
        }
        if (ticketproException.result() == TicketproCheckResult.ANTIBOT) {
            activateBackoff(ANTI_BOT_BACKOFF, TicketproCheckResult.ANTIBOT);
        }
    }

    private void activateBackoff(Duration duration, TicketproCheckResult reason) {
        backoffReason.set(reason);
        backoff.activate(limitBackoff(duration));
    }

    private Duration parseRetryAfter(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_RATE_LIMIT_BACKOFF;
        }
        try {
            long seconds = Long.parseLong(value.trim());
            return seconds < 0 ? DEFAULT_RATE_LIMIT_BACKOFF : Duration.ofSeconds(seconds);
        } catch (NumberFormatException ignored) {
            try {
                ZonedDateTime retryAt = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
                Duration duration = Duration.between(clock.instant(), retryAt.toInstant());
                return duration.isNegative() ? Duration.ZERO : duration;
            } catch (DateTimeParseException ignoredDate) {
                return DEFAULT_RATE_LIMIT_BACKOFF;
            }
        }
    }

    private Duration limitBackoff(Duration duration) {
        return duration.compareTo(MAXIMUM_BACKOFF) > 0 ? MAXIMUM_BACKOFF : duration;
    }
}
