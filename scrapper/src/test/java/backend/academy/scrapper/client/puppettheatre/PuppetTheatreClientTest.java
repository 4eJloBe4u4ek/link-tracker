package backend.academy.scrapper.client.puppettheatre;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PuppetTheatreClientTest {
    private static final Instant NOW = Instant.parse("2026-09-14T10:00:00Z");

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private MutableClock clock;
    private PuppetTheatreClient client;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(NOW);
        URI afishaUri = URI.create(wireMock.baseUrl() + "/afisha");
        client = new PuppetTheatreClient(
                new PuppetTheatrePageParser(afishaUri), afishaUri, clock, Duration.ofSeconds(15));
    }

    @Test
    void shouldFetchOnlyAfishaCardsWithExpectedHeaders() {
        stubAfisha(validPage());

        PuppetTheatrePage page = client.getAvailableSessions().block();

        assertThat(page.sessions()).hasSize(1);
        wireMock.verify(getRequestedFor(urlEqualTo("/afisha"))
                .withHeader(HttpHeaders.ACCEPT, equalTo("text/html,application/xhtml+xml"))
                .withHeader(HttpHeaders.ACCEPT_LANGUAGE, equalTo("ru-BY,ru;q=0.9,en;q=0.5"))
                .withHeader(HttpHeaders.USER_AGENT, notMatching(".*Chrome.*")));
    }

    @Test
    void shouldPersistServerCookieInTheClientSession() {
        wireMock.stubFor(get(urlEqualTo("/afisha"))
                .inScenario("session")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withHeader(HttpHeaders.SET_COOKIE, "theatre_session=abc123; Path=/; HttpOnly")
                        .withBody(validPage()))
                .willSetStateTo("cookie-issued"));
        wireMock.stubFor(get(urlEqualTo("/afisha"))
                .inScenario("session")
                .whenScenarioStateIs("cookie-issued")
                .withHeader(HttpHeaders.COOKIE, equalTo("theatre_session=abc123"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withBody(validPage())));

        client.getAvailableSessions().block();
        client.getAvailableSessions().block();

        wireMock.verify(
                1,
                getRequestedFor(urlEqualTo("/afisha"))
                        .withHeader(HttpHeaders.COOKIE, equalTo("theatre_session=abc123")));
    }

    @Test
    void shouldAllowOneRegularRetryThenBackOffForTwoMinutes() {
        wireMock.stubFor(get(urlEqualTo("/afisha")).willReturn(aResponse().withStatus(403)));

        verifyFailure(client.getAvailableSessions(), PuppetTheatreCheckResult.HTTP_403);
        verifyFailure(client.getAvailableSessions(), PuppetTheatreCheckResult.HTTP_403);
        verifyFailure(client.getAvailableSessions(), PuppetTheatreCheckResult.SKIPPED_BACKOFF);
        wireMock.verify(2, getRequestedFor(urlEqualTo("/afisha")));
        clock.advance(Duration.ofMinutes(2));
        verifyFailure(client.getAvailableSessions(), PuppetTheatreCheckResult.HTTP_403);
        wireMock.verify(3, getRequestedFor(urlEqualTo("/afisha")));
    }

    @Test
    void shouldUseRegularRetryForFirstRateLimitWithoutRetryAfter() {
        wireMock.stubFor(get(urlEqualTo("/afisha")).willReturn(aResponse().withStatus(429)));

        verifyFailure(client.getAvailableSessions(), PuppetTheatreCheckResult.HTTP_429);
        verifyFailure(client.getAvailableSessions(), PuppetTheatreCheckResult.HTTP_429);

        wireMock.verify(2, getRequestedFor(urlEqualTo("/afisha")));
    }

    @Test
    void shouldHonorRetryAfterAndRejectCrossOriginRedirect() {
        wireMock.stubFor(get(urlEqualTo("/afisha"))
                .willReturn(aResponse().withStatus(429).withHeader(HttpHeaders.RETRY_AFTER, "600")));

        verifyFailure(client.getAvailableSessions(), PuppetTheatreCheckResult.HTTP_429);
        verifyFailure(client.getAvailableSessions(), PuppetTheatreCheckResult.SKIPPED_BACKOFF);
        clock.advance(Duration.ofMinutes(10));
        wireMock.stubFor(get(urlEqualTo("/afisha"))
                .willReturn(
                        aResponse().withStatus(302).withHeader(HttpHeaders.LOCATION, "https://example.com/challenge")));
        verifyFailure(client.getAvailableSessions(), PuppetTheatreCheckResult.ANTIBOT);
    }

    @Test
    void shouldClassifyRequestTimeout() {
        wireMock.stubFor(get(urlEqualTo("/afisha"))
                .willReturn(aResponse()
                        .withFixedDelay(500)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withBody(validPage())));
        URI afishaUri = URI.create(wireMock.baseUrl() + "/afisha");
        PuppetTheatreClient shortTimeoutClient = new PuppetTheatreClient(
                new PuppetTheatrePageParser(afishaUri), afishaUri, clock, Duration.ofMillis(50));

        verifyFailure(shortTimeoutClient.getAvailableSessions(), PuppetTheatreCheckResult.TIMEOUT);
    }

    @Test
    void shouldRejectResponseLargerThanConfiguredLimit() {
        stubAfisha("x".repeat(2 * 1024 * 1024 + 1));

        verifyFailure(client.getAvailableSessions(), PuppetTheatreCheckResult.PARSE_ERROR);
    }

    @Test
    void shouldRejectNonHtmlResponse() {
        wireMock.stubFor(get(urlEqualTo("/afisha"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{}")));

        verifyFailure(client.getAvailableSessions(), PuppetTheatreCheckResult.PARSE_ERROR);
    }

    @Test
    void shouldReturnEmptyPageWhenTheParserDoesNotRecognizeThePage() {
        String unsupportedPage = "<html><body><a href=\"/tickets\">Tickets</a></body></html>";
        stubAfisha(unsupportedPage);

        verifyFailure(client.getAvailableSessions(), PuppetTheatreCheckResult.EMPTY_PAGE);
    }

    private void stubAfisha(String body) {
        wireMock.stubFor(get(urlEqualTo("/afisha"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withBody(body)));
    }

    private String validPage() {
        return """
                <div class="afisha_item item_mounth-2026-09">
                  <div class="afisha-day">19 Сентября, Сб</div><div class="afisha-time">11:00</div>
                  <div class="afisha-title">Волшебное путешествие</div>
                  <a class="afisha_item-hover" href="/spektakli/volshebnoe-puteshestvie#tickets"></a>
                </div>
                """;
    }

    private void verifyFailure(Mono<PuppetTheatrePage> result, PuppetTheatreCheckResult expected) {
        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(PuppetTheatreException.class);
                    assertThat(((PuppetTheatreException) error).result()).isEqualTo(expected);
                })
                .verify();
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
