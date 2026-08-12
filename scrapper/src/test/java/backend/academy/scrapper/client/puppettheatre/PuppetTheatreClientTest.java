package backend.academy.scrapper.client.puppettheatre;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.scrapper.config.ScrapperConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PuppetTheatreClientTest {
    private static final String VENUE_PATH = "/venue/puppet-theatre/";
    private static final Instant NOW = Instant.parse("2026-08-10T15:00:00Z");

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private PuppetTheatreClient client;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        ScrapperConfig.PuppetTheatre puppetTheatre = new ScrapperConfig.PuppetTheatre(wireMock.baseUrl(), VENUE_PATH);
        ScrapperConfig config = new ScrapperConfig(null, null, 100, null, null, null, puppetTheatre, null);
        clock = new MutableClock(NOW);
        client = new PuppetTheatreClient(
                config, new TicketproEventParser(config, new ObjectMapper()), clock, Duration.ofSeconds(15));
    }

    @Test
    void shouldBackOffForFifteenMinutesAfterForbiddenResponse() {
        // Arrange
        wireMock.stubFor(get(urlEqualTo(VENUE_PATH)).willReturn(aResponse().withStatus(403)));

        // Act & Assert
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.FORBIDDEN);
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.FORBIDDEN);
        wireMock.verify(1, getRequestedFor(urlEqualTo(VENUE_PATH)));
        clock.advance(Duration.ofMinutes(15));
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.FORBIDDEN);
        wireMock.verify(2, getRequestedFor(urlEqualTo(VENUE_PATH)));
    }

    @Test
    void shouldHonorRetryAfterSeconds() {
        // Arrange
        wireMock.stubFor(get(urlEqualTo(VENUE_PATH))
                .willReturn(aResponse().withStatus(429).withHeader(HttpHeaders.RETRY_AFTER, "120")));

        // Act & Assert
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.RATE_LIMITED);
        clock.advance(Duration.ofSeconds(119));
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.RATE_LIMITED);
        wireMock.verify(1, getRequestedFor(urlEqualTo(VENUE_PATH)));
        clock.advance(Duration.ofSeconds(1));
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.RATE_LIMITED);
        wireMock.verify(2, getRequestedFor(urlEqualTo(VENUE_PATH)));
    }

    @Test
    void shouldUseFiveMinuteBackoffForInvalidRetryAfter() {
        // Arrange
        wireMock.stubFor(get(urlEqualTo(VENUE_PATH))
                .willReturn(aResponse().withStatus(429).withHeader(HttpHeaders.RETRY_AFTER, "invalid")));

        // Act & Assert
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.RATE_LIMITED);
        clock.advance(Duration.ofMinutes(5).minusSeconds(1));
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.RATE_LIMITED);
        wireMock.verify(1, getRequestedFor(urlEqualTo(VENUE_PATH)));
        clock.advance(Duration.ofSeconds(1));
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.RATE_LIMITED);
        wireMock.verify(2, getRequestedFor(urlEqualTo(VENUE_PATH)));
    }

    @Test
    void shouldHonorRetryAfterHttpDate() {
        // Arrange
        String retryAfter = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                ZonedDateTime.ofInstant(NOW.plusSeconds(120), ZoneId.of("GMT")));
        wireMock.stubFor(get(urlEqualTo(VENUE_PATH))
                .willReturn(aResponse().withStatus(429).withHeader(HttpHeaders.RETRY_AFTER, retryAfter)));

        // Act & Assert
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.RATE_LIMITED);
        clock.advance(Duration.ofSeconds(119));
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.RATE_LIMITED);
        wireMock.verify(1, getRequestedFor(urlEqualTo(VENUE_PATH)));
        clock.advance(Duration.ofSeconds(1));
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.RATE_LIMITED);
        wireMock.verify(2, getRequestedFor(urlEqualTo(VENUE_PATH)));
    }

    @Test
    void shouldCapRetryAfterAtOneHour() {
        // Arrange
        wireMock.stubFor(get(urlEqualTo(VENUE_PATH))
                .willReturn(aResponse().withStatus(429).withHeader(HttpHeaders.RETRY_AFTER, "7200")));

        // Act & Assert
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.RATE_LIMITED);
        clock.advance(Duration.ofHours(1).minusSeconds(1));
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.RATE_LIMITED);
        wireMock.verify(1, getRequestedFor(urlEqualTo(VENUE_PATH)));
        clock.advance(Duration.ofSeconds(1));
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.RATE_LIMITED);
        wireMock.verify(2, getRequestedFor(urlEqualTo(VENUE_PATH)));
    }

    @Test
    void shouldBackOffAfterAntiBotChallenge() {
        // Arrange
        stubVenuePage("<div>Protected by Anubis</div>");

        // Act & Assert
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.ANTIBOT);
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.ANTIBOT);
        wireMock.verify(1, getRequestedFor(urlEqualTo(VENUE_PATH)));
        clock.advance(Duration.ofMinutes(15));
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.ANTIBOT);
        wireMock.verify(2, getRequestedFor(urlEqualTo(VENUE_PATH)));
    }

    @Test
    void shouldClassifyRequestTimeout() {
        // Arrange
        wireMock.stubFor(get(urlEqualTo(VENUE_PATH))
                .willReturn(aResponse()
                        .withFixedDelay(500)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withBody("<html></html>")));
        ScrapperConfig.PuppetTheatre properties = new ScrapperConfig.PuppetTheatre(wireMock.baseUrl(), VENUE_PATH);
        ScrapperConfig config = new ScrapperConfig(null, null, 100, null, null, null, properties, null);
        PuppetTheatreClient shortTimeoutClient = new PuppetTheatreClient(
                config, new TicketproEventParser(config, new ObjectMapper()), clock, Duration.ofMillis(100));

        // Act & Assert
        verifyFailureResult(shortTimeoutClient.getAvailableSessions(), TicketproCheckResult.TIMEOUT);
    }

    @Test
    void shouldClassifyNetworkFailure() {
        // Arrange
        ScrapperConfig.PuppetTheatre properties =
                new ScrapperConfig.PuppetTheatre("http://127.0.0.1:1", VENUE_PATH);
        ScrapperConfig config = new ScrapperConfig(null, null, 100, null, null, null, properties, null);
        PuppetTheatreClient unavailableClient = new PuppetTheatreClient(
                config, new TicketproEventParser(config, new ObjectMapper()), clock, Duration.ofSeconds(2));

        // Act & Assert
        verifyFailureResult(unavailableClient.getAvailableSessions(), TicketproCheckResult.NETWORK_ERROR);
    }

    @Test
    void shouldIncludeUnexpectedHttpStatusInErrorMessage() {
        // Arrange
        wireMock.stubFor(get(urlEqualTo(VENUE_PATH)).willReturn(aResponse().withStatus(502)));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectErrorMatches(error -> error instanceof TicketproException ticketproException
                        && ticketproException.result() == TicketproCheckResult.NETWORK_ERROR
                        && ticketproException.getMessage().contains("502"))
                .verify();
    }

    @Test
    void shouldReadInStockEventsFromTicketproJsonLd() {
        // Arrange
        String eventUrl = wireMock.baseUrl() + "/bilety-v-teatr/kot-v-sapogah/";
        stubVenuePage(
                """
                <html>
                  <body>
                    <h1>Белорусский государственный театр кукол</h1>
                    <div class="event-box"></div>
                    <script type="application/ld+json">
                    {
                      "@context": "https://schema.org",
                      "@type": "Event",
                      "url": "%s",
                      "name": "Кот &quot;в сапогах&quot;",
                      "startDate": "2026-09-12T11:00:00+0300",
                      "endDate": "2026-09-13",
                      "location": {
                        "@type": "Place",
                        "name": "Белорусский государственный театр кукол"
                      },
                      "offers": {
                        "@type": "AggregateOffer",
                        "availability": "http://schema.org/InStock",
                        "lowPrice": "38.00",
                        "highPrice": "40.00",
                        "priceCurrency": "BYN"
                      }
                    }
                    </script>
                  </body>
                </html>
                """
                        .formatted(eventUrl));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectNextMatches(sessions -> sessions.size() == 1
                        && sessions.getFirst().title().equals("Кот \"в сапогах\"")
                        && sessions.getFirst().date().equals("12.09.2026–13.09.2026")
                        && sessions.getFirst().time().equals("11:00")
                        && sessions.getFirst().price().equals("38–40 BYN")
                        && sessions.getFirst().ticketUrl().equals(eventUrl))
                .verifyComplete();
    }

    @Test
    void shouldReadJsonLdFromValidUnquotedScriptType() {
        // Arrange
        stubVenuePage(
                """
                <html>
                  <body>
                    <h1>Белорусский государственный театр кукол</h1>
                    <div class="event-box"></div>
                    <script type=application/ld+json>%s</script>
                  </body>
                </html>
                """
                        .formatted(eventJson("buratino", "Буратино", "2026-09-12T11:00:00+03:00")));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectNextMatches(sessions -> sessions.size() == 1 && sessions.getFirst().title().equals("Буратино"))
                .verifyComplete();
    }

    @Test
    void shouldReadEventWithFullSchemaOrgType() {
        // Arrange
        stubVenuePage(validVenuePage(eventJson(
                "buratino",
                "Буратино",
                "2026-09-12T11:00:00+03:00",
                "https://schema.org/Event",
                "https://schema.org/InStock")));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectNextMatches(sessions ->
                        sessions.size() == 1 && sessions.getFirst().title().equals("Буратино"))
                .verifyComplete();
    }

    @Test
    void shouldIgnoreInStockValueFromAnotherVocabulary() {
        // Arrange
        stubVenuePage(validVenuePage(eventJson(
                "buratino",
                "Буратино",
                "2026-09-12T11:00:00+03:00",
                "Event",
                "https://example.com/InStock")));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    void shouldReadSeveralEventsInOneVenueRequest() {
        // Arrange
        stubVenuePage(
                """
                <html>
                  <body>
                    <h1>Белорусский государственный театр кукол</h1>
                    <div class="event-box"></div>
                    <div class="event-box"></div>
                    <script type="application/ld+json">
                    [
                      {
                        "@type": "Event",
                        "url": "%s/bilety-v-teatr/buratino/",
                        "name": "Буратино",
                        "startDate": "2026-09-12T11:00:00+03:00",
                        "endDate": "2026-09-12",
                        "location": {"name": "Белорусский государственный театр кукол"},
                        "offers": {"availability": "http://schema.org/InStock"}
                      },
                      {
                        "@type": "Event",
                        "url": "%s/bilety-v-teatr/mojdodyr/",
                        "name": "Мойдодыр",
                        "startDate": "2026-09-13T12:00:00+03:00",
                        "endDate": "2026-09-13",
                        "location": {"name": "Белорусский государственный театр кукол"},
                        "offers": {"availability": "https://schema.org/InStock"}
                      }
                    ]
                    </script>
                  </body>
                </html>
                """
                        .formatted(wireMock.baseUrl(), wireMock.baseUrl()));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectNextMatches(sessions -> sessions.size() == 2
                        && sessions.stream()
                                .map(PuppetTheatreSession::title)
                                .toList()
                                .containsAll(List.of("Буратино", "Мойдодыр")))
                .verifyComplete();
    }

    @Test
    void shouldReadEventsFromAllVenuePagesAndRemoveDuplicates() {
        // Arrange
        String firstEvent = eventJson("buratino", "Буратино", "2026-09-12T11:00:00+03:00");
        String secondEvent = eventJson("mojdodyr", "Мойдодыр", "2026-09-13T12:00:00+03:00");
        stubVenuePage(
                1,
                """
                <html>
                  <head>
                    <link href="%s?page=2" rel="last">
                  </head>
                  <body>
                    <h1>Белорусский государственный театр кукол</h1>
                    <div class="event-box"></div>
                    <script type="application/ld+json">%s</script>
                  </body>
                </html>
                """
                        .formatted(VENUE_PATH, firstEvent));
        stubVenuePage(
                2,
                """
                <html>
                  <body>
                    <h1>Белорусский государственный театр кукол</h1>
                    <div class="event-box"></div>
                    <div class="event-box"></div>
                    <script type="application/ld+json">[%s, %s]</script>
                  </body>
                </html>
                """
                        .formatted(firstEvent, secondEvent));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectNextMatches(sessions -> sessions.size() == 2
                        && sessions.stream()
                                .map(PuppetTheatreSession::title)
                                .toList()
                                .equals(List.of("Буратино", "Мойдодыр")))
                .verifyComplete();
    }

    @Test
    void shouldFailTheWholeCheckWhenAFollowingPageFails() {
        // Arrange
        stubVenuePage(
                1,
                """
                <html>
                  <head><link rel="last" href="%s?page=2"></head>
                  <body><h1>Белорусский государственный театр кукол</h1></body>
                </html>
                        """
                        .formatted(VENUE_PATH));
        wireMock.stubFor(
                get(urlEqualTo(VENUE_PATH + "?page=2")).willReturn(aResponse().withStatus(503)));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions()).expectError().verify();
    }

    @Test
    void shouldIgnoreEventsThatAreNotInStock() {
        // Arrange
        stubVenuePage(
                """
                <html>
                  <body>
                    <h1>Белорусский государственный театр кукол</h1>
                    <div class="event-box"></div>
                    <script type="application/ld+json">
                    {
                      "@type": "Event",
                      "url": "%s/bilety-v-teatr/sold-out/",
                      "name": "Проданный смех",
                      "startDate": "2026-09-12T19:00:00+0300",
                      "endDate": "2026-09-12",
                      "location": {"name": "Белорусский государственный театр кукол"},
                      "offers": {"availability": "https://schema.org/SoldOut"}
                    }
                    </script>
                  </body>
                </html>
                """
                        .formatted(wireMock.baseUrl()));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectNextMatches(sessions -> sessions.isEmpty())
                .verifyComplete();
    }

    @Test
    void shouldTreatVenuePageWithoutEventsAsValidEmptyCatalog() {
        // Arrange
        stubVenuePage(
                """
                <html>
                  <body><h1>Белорусский государственный театр кукол</h1></body>
                </html>
                """);

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectNextMatches(sessions -> sessions.isEmpty())
                .verifyComplete();
    }

    @Test
    void shouldFailWhenBotProtectionReturnsAnUnexpectedPage() {
        // Arrange
        stubVenuePage("<html><title>Making sure you're not a bot!</title></html>");

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("unexpected page"))
                .verify();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "<div>Protected by Anubis</div>",
                "<script>window.Imunify360 = true;</script>",
                "<h2>Checking your browser before accessing Ticketpro</h2>"
            })
    void shouldRejectKnownChallengeEvenWhenVenueMarkerIsPresent(String challengeFixture) {
        // Arrange
        stubVenuePage(
                """
        <html>
          <body>
            <h1>Белорусский государственный театр кукол</h1>
            %s
          </body>
        </html>
        """
                        .formatted(challengeFixture));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("anti-bot challenge"))
                .verify();
    }

    @Test
    void shouldFailWhenJsonLdIsMalformed() {
        // Arrange
        stubVenuePage(
                """
                <html>
                  <body>
                    <h1>Белорусский государственный театр кукол</h1>
                    <script type="application/ld+json">{not-json}</script>
                  </body>
                </html>
                """);

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("malformed JSON-LD"))
                .verify();
    }

    @Test
    void shouldFailWhenTicketproReturnsEmptyBody() {
        // Arrange
        stubVenuePage("");

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("empty HTML"))
                .verify();
    }

    @Test
    void shouldFailWhenTicketproReturnsNonHtmlContent() {
        // Arrange
        wireMock.stubFor(get(urlEqualTo(VENUE_PATH))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(validVenuePage(eventJson("buratino", "Буратино", "2026-09-12T11:00:00+03:00")))));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("non-HTML"))
                .verify();
    }

    @Test
    void shouldClassifyOversizedHtmlAsInvalidContent() {
        // Arrange
        wireMock.stubFor(get(urlEqualTo(VENUE_PATH))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withBody("x".repeat(3 * 1024 * 1024))));

        // Act & Assert
        verifyFailureResult(client.getAvailableSessions(), TicketproCheckResult.INVALID_CONTENT);
    }

    @Test
    void shouldAcceptTicketUrlFromSameSchemeAndHostWithDifferentPort() {
        // Arrange
        URI base = URI.create(wireMock.baseUrl());
        int differentPort = base.getPort() == 65535 ? 65534 : base.getPort() + 1;
        String eventUrl = "%s://%s:%d/bilety-v-teatr/buratino/"
                .formatted(base.getScheme(), base.getHost(), differentPort);
        stubVenuePage(
                validVenuePage(eventJsonWithUrl(eventUrl, "Буратино", "2026-09-12T11:00:00+03:00", "2026-09-12")));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectNextMatches(sessions ->
                        sessions.size() == 1 && sessions.getFirst().ticketUrl().equals(eventUrl))
                .verifyComplete();
    }

    @Test
    void shouldRejectTicketUrlFromAnotherOrigin() {
        // Arrange
        URI base = URI.create(wireMock.baseUrl());
        String anotherOrigin = "https://" + base.getHost() + ":" + base.getPort() + "/bilety-v-teatr/buratino/";
        stubVenuePage(
                validVenuePage(eventJsonWithUrl(anotherOrigin, "Буратино", "2026-09-12T11:00:00+03:00", "2026-09-12")));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("unexpected origin"))
                .verify();
    }

    @Test
    void shouldFailWhenEndDatePrecedesStartDate() {
        // Arrange
        stubVenuePage(validVenuePage(eventJsonWithUrl(
                wireMock.baseUrl() + "/bilety-v-teatr/buratino/",
                "Буратино",
                "2026-09-13T11:00:00+03:00",
                "2026-09-12")));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("invalid date range"))
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-09-12T11:00:99+03:00", "2026-09-12T11:00:00+25:00", "2026-09-12T11:00:00+03:00junk"})
    void shouldRejectInvalidWholeStartDate(String startDate) {
        // Arrange
        stubVenuePage(validVenuePage(eventJsonWithUrl(
                wireMock.baseUrl() + "/bilety-v-teatr/buratino/", "Буратино", startDate, "2026-09-12")));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("invalid date"))
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-09-12T11:00:99+0300", "2026-09-12T11:00:00+2500", "2026-09-12T11:00:00+0300junk"})
    void shouldRejectInvalidWholeEndDate(String endDate) {
        // Arrange
        stubVenuePage(validVenuePage(eventJsonWithUrl(
                wireMock.baseUrl() + "/bilety-v-teatr/buratino/", "Буратино", "2026-09-12T11:00:00+03:00", endDate)));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("invalid date"))
                .verify();
    }

    @Test
    void shouldSendOnlyHtmlAcceptApplicationHeader() {
        // Arrange
        stubVenuePage(validVenuePage(eventJson("buratino", "Буратино", "2026-09-12T11:00:00+03:00")));

        // Act
        StepVerifier.create(client.getAvailableSessions()).expectNextCount(1).verifyComplete();

        // Assert
        wireMock.verify(getRequestedFor(urlEqualTo(VENUE_PATH))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_HTML_VALUE))
                .withHeader(HttpHeaders.USER_AGENT, notMatching("LinkTracker/1[.]0"))
                .withoutHeader(HttpHeaders.ACCEPT_LANGUAGE));
    }

    @Test
    void shouldRejectPaginationAboveSafetyLimit() {
        // Arrange
        stubVenuePage(
                """
                <html>
                  <head><link rel="last" href="%s?page=21"></head>
                  <body><h1>Белорусский государственный театр кукол</h1></body>
                </html>
                """
                        .formatted(VENUE_PATH));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("too many venue pages"))
                .verify();
    }

    @Test
    void shouldAcceptRelativeTicketUrlFromConfiguredOrigin() {
        // Arrange
        stubVenuePage(validVenuePage(
                eventJsonWithUrl("/bilety-v-teatr/buratino/", "Буратино", "2026-09-12T11:00:00+03:00", "2026-09-12")));

        // Act & Assert
        StepVerifier.create(client.getAvailableSessions())
                .expectNextMatches(sessions -> sessions.size() == 1
                        && sessions.getFirst().ticketUrl().equals(wireMock.baseUrl() + "/bilety-v-teatr/buratino/"))
                .verifyComplete();
    }

    private void stubVenuePage(String body) {
        stubVenuePage(1, body);
    }

    private void stubVenuePage(int page, String body) {
        String url = page == 1 ? VENUE_PATH : VENUE_PATH + "?page=" + page;
        wireMock.stubFor(get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withBody(body)));
    }

    private String eventJson(String slug, String title, String startDate) {
        return eventJson(slug, title, startDate, "Event", "http://schema.org/InStock");
    }

    private String eventJson(String slug, String title, String startDate, String type, String availability) {
        return """
        {
          "@type": "%s",
          "url": "%s/bilety-v-teatr/%s/",
          "name": "%s",
          "startDate": "%s",
          "location": {"name": "Белорусский государственный театр кукол"},
          "offers": {"availability": "%s"}
        }
        """
                .formatted(type, wireMock.baseUrl(), slug, title, startDate, availability);
    }

    private String validVenuePage(String jsonLd) {
        return """
        <html>
          <body>
            <h1>Белорусский государственный театр кукол</h1>
            <div class="event-box"></div>
            <script type="application/ld+json">%s</script>
          </body>
        </html>
        """
                .formatted(jsonLd);
    }

    private String eventJsonWithUrl(String url, String title, String startDate, String endDate) {
        return """
        {
          "@type": "Event",
          "url": "%s",
          "name": "%s",
          "startDate": "%s",
          "endDate": "%s",
          "location": {"name": "Белорусский государственный театр кукол"},
          "offers": {"availability": "http://schema.org/InStock"}
        }
        """
                .formatted(url, title, startDate, endDate);
    }

    private void verifyFailureResult(Mono<List<PuppetTheatreSession>> result, TicketproCheckResult expectedResult) {
        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(TicketproException.class);
                    assertThat(((TicketproException) error).result()).isEqualTo(expectedResult);
                })
                .verify();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
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
            return instant;
        }
    }
}
