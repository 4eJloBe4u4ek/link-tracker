package backend.academy.scrapper.client.puppettheatre;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Component
public class PuppetTheatreClient {
    public static final URI AFISHA_URI = URI.create("https://puppet-minsk.by/afisha");

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration DEFAULT_RETRY_AFTER = Duration.ZERO;
    private static final int PAGE_MAX_IN_MEMORY_SIZE = 2 * 1024 * 1024;

    private final PuppetTheatrePageParser pageParser;
    private final URI afishaUri;
    private final Clock clock;
    private final Duration requestTimeout;
    private final PuppetTheatreBackoff backoff;
    private final WebClient puppetTheatreClient;
    private final Map<String, ResponseCookie> sessionCookies = new ConcurrentHashMap<>();

    @Autowired
    public PuppetTheatreClient(PuppetTheatrePageParser pageParser) {
        this(pageParser, AFISHA_URI, Clock.systemUTC(), REQUEST_TIMEOUT);
    }

    PuppetTheatreClient(
            PuppetTheatrePageParser pageParser,
            URI afishaUri,
            Clock clock,
            Duration requestTimeout) {
        this.pageParser = pageParser;
        this.afishaUri = afishaUri;
        this.clock = clock;
        this.requestTimeout = requestTimeout;
        this.backoff = new PuppetTheatreBackoff(clock);
        this.puppetTheatreClient = WebClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "ru-BY,ru;q=0.9,en;q=0.5")
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(PAGE_MAX_IN_MEMORY_SIZE))
                        .build())
                .build();
    }

    public Mono<PuppetTheatrePage> getAvailableSessions() {
        return Mono.defer(() -> {
                    if (!backoff.requestAllowed()) {
                        return Mono.error(new PuppetTheatreException(
                                PuppetTheatreCheckResult.SKIPPED_BACKOFF, "Puppet theatre check is in backoff"));
                    }
                    return fetchAfisha().map(pageParser::parse);
                })
                .timeout(requestTimeout)
                .onErrorMap(this::classifyFailure)
                .doOnError(this::recordBlockingFailure)
                .doOnSuccess(ignored -> backoff.recordSuccess());
    }

    private Mono<String> fetchAfisha() {
        return puppetTheatreClient
                .get()
                .uri(afishaUri)
                .headers(headers -> cookieHeader().ifPresent(value -> headers.set(HttpHeaders.COOKIE, value)))
                .exchangeToMono(this::handleResponse);
    }

    private Mono<String> handleResponse(ClientResponse response) {
        rememberCookies(response);
        HttpStatusCode status = response.statusCode();
        if (status.is3xxRedirection()) {
            return fail(PuppetTheatreCheckResult.ANTIBOT, "Puppet theatre returned an unsafe redirect", Duration.ZERO);
        }
        if (status.isSameCodeAs(HttpStatus.FORBIDDEN)) {
            return fail(PuppetTheatreCheckResult.HTTP_403, "Puppet theatre request forbidden", Duration.ZERO);
        }
        if (status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
            String retryAfter = response.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);
            return fail(
                    PuppetTheatreCheckResult.HTTP_429,
                    "Puppet theatre rate limit exceeded",
                    parseRetryAfter(retryAfter));
        }
        if (!status.is2xxSuccessful()) {
            return fail(
                    PuppetTheatreCheckResult.HTTP_ERROR,
                    "Puppet theatre returned HTTP " + status.value(),
                    Duration.ZERO);
        }
        return readHtml(response);
    }

    private Mono<String> readHtml(ClientResponse response) {
        MediaType contentType = response.headers().contentType().orElse(null);
        if (contentType == null || !MediaType.TEXT_HTML.isCompatibleWith(contentType)) {
            return fail(
                    PuppetTheatreCheckResult.PARSE_ERROR, "Puppet theatre returned non-HTML content", Duration.ZERO);
        }
        return response.bodyToMono(String.class)
                .filter(body -> !body.isBlank())
                .switchIfEmpty(
                        fail(PuppetTheatreCheckResult.EMPTY_PAGE, "Puppet theatre returned empty HTML", Duration.ZERO));
    }

    private void rememberCookies(ClientResponse response) {
        response.cookies().forEach((name, values) -> {
            if (values.isEmpty()) {
                return;
            }
            ResponseCookie cookie = values.getLast();
            if (cookie.getMaxAge().isZero()) {
                sessionCookies.remove(name);
            } else {
                sessionCookies.put(name, cookie);
            }
        });
    }

    private Optional<String> cookieHeader() {
        if (sessionCookies.isEmpty()) {
            return Optional.empty();
        }
        String value = sessionCookies.values().stream()
                .sorted(Comparator.comparing(HttpCookie::getName))
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; "));
        return Optional.of(value);
    }

    private Throwable classifyFailure(Throwable error) {
        if (error instanceof PuppetTheatreException) {
            return error;
        }
        if (error instanceof TimeoutException) {
            return new PuppetTheatreException(
                    PuppetTheatreCheckResult.TIMEOUT, "Puppet theatre request timed out", error);
        }
        if (error instanceof DataBufferLimitException) {
            return new PuppetTheatreException(
                    PuppetTheatreCheckResult.PARSE_ERROR, "Puppet theatre response is too large", error);
        }
        if (error instanceof WebClientRequestException) {
            return new PuppetTheatreException(
                    PuppetTheatreCheckResult.NETWORK_ERROR, "Puppet theatre network request failed", error);
        }
        return new PuppetTheatreException(
                PuppetTheatreCheckResult.PARSE_ERROR, "Puppet theatre response could not be parsed", error);
    }

    private void recordBlockingFailure(Throwable error) {
        if (!(error instanceof PuppetTheatreException exception)) {
            return;
        }
        if (exception.result() == PuppetTheatreCheckResult.HTTP_403
                || exception.result() == PuppetTheatreCheckResult.HTTP_429
                || exception.result() == PuppetTheatreCheckResult.ANTIBOT) {
            backoff.recordBlockingFailure(exception.retryAfter());
        }
    }

    private Duration parseRetryAfter(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_RETRY_AFTER;
        }
        try {
            long seconds = Long.parseLong(value.trim());
            return seconds < 0 ? DEFAULT_RETRY_AFTER : Duration.ofSeconds(seconds);
        } catch (NumberFormatException ignored) {
            try {
                ZonedDateTime retryAt = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
                Duration duration = Duration.between(clock.instant(), retryAt.toInstant());
                return duration.isNegative() ? Duration.ZERO : duration;
            } catch (DateTimeParseException ignoredDate) {
                return DEFAULT_RETRY_AFTER;
            }
        }
    }

    private Mono<String> fail(PuppetTheatreCheckResult result, String message, Duration retryAfter) {
        return Mono.error(new PuppetTheatreException(result, message, retryAfter));
    }
}
