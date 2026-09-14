package backend.academy.scrapper.client.ticketpro;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.scrapper.config.ScrapperConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class TicketproClientTest {
    private static final String VENUE_PATH = "/venue/ticketpro/";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private TicketproClient client;

    @BeforeEach
    void setUp() {
        ScrapperConfig.Ticketpro ticketpro = new ScrapperConfig.Ticketpro(wireMock.baseUrl());
        ScrapperConfig config = new ScrapperConfig(null, null, 100, null, null, null, ticketpro, null);
        client = new TicketproClient(config, new TicketproEventParser(config, new ObjectMapper()));
    }

    @Test
    void shouldRequestTrackedVenueInsteadOfConfiguredVenue() {
        // Arrange
        String trackedPath = "/koncertnye-ploshhadki/dvorec-respubliki/";
        stubVenue(trackedPath, "ГУ Дворец Республики, Минск");
        String venueUrl = wireMock.baseUrl() + trackedPath;

        // Act
        TicketproVenue result = client.getAvailableEvents(venueUrl).block();

        // Assert
        assertThat(result.name()).isEqualTo("ГУ Дворец Республики, Минск");
        wireMock.verify(getRequestedFor(urlEqualTo(trackedPath)));
        wireMock.verify(0, getRequestedFor(urlEqualTo(VENUE_PATH)));
    }

    @Test
    void shouldPreservePercentEncodingInTrackedVenueRawPath() {
        // Arrange
        String encodedPath = "/venue%20encoded/";
        wireMock.stubFor(get(anyUrl())
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withBody("<html><body><h1>Encoded venue</h1></body></html>")));
        String venueUrl = wireMock.baseUrl() + encodedPath;

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(venue -> venue.name().equals("Encoded venue"))
                .verifyComplete();
        wireMock.verify(getRequestedFor(urlEqualTo(encodedPath)));
        wireMock.verify(0, getRequestedFor(urlEqualTo("/venue%2520encoded/")));
    }

    @Test
    void shouldReadSeveralEventsInOneVenueRequest() {
        // Arrange
        stubVenuePage(
                """
                <html>
                  <body>
                    <h1>Белорусский государственный театр кукол</h1>
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

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl());

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(venue -> venue.name().equals("Белорусский государственный театр кукол")
                        && venue.events().size() == 2
                        && venue.events().stream()
                                .map(TicketproEvent::title)
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
                    <script type="application/ld+json">[%s, %s]</script>
                  </body>
                </html>
                """
                        .formatted(firstEvent, secondEvent));

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl());

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(venue -> venue.events().size() == 2
                        && venue.events().stream()
                                .map(TicketproEvent::title)
                                .toList()
                                .equals(List.of("Буратино", "Мойдодыр")))
                .verifyComplete();
        wireMock.verify(getRequestedFor(urlEqualTo(VENUE_PATH)));
        wireMock.verify(getRequestedFor(urlEqualTo(VENUE_PATH + "?page=2")));
        wireMock.verify(0, getRequestedFor(urlPathEqualTo(VENUE_PATH)).withQueryParam("page", equalTo("1")));
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

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl());

        // Assert
        StepVerifier.create(result)
                .expectError(WebClientResponseException.class)
                .verify();
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

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl());

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(venue -> venue.events().isEmpty())
                .verifyComplete();
    }

    @Test
    void shouldFailWhenBotProtectionReturnsAnUnexpectedPage() {
        // Arrange
        stubVenuePage("<html><title>Making sure you're not a bot!</title></html>");

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl());

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("no heading"))
                .verify();
    }

    @Test
    void shouldFailWhenTicketproReturnsEmptyBody() {
        // Arrange
        stubVenuePage("");

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl());

        // Assert
        StepVerifier.create(result)
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

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl());

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("non-HTML"))
                .verify();
    }

    @Test
    void shouldRejectTicketUrlFromSameSchemeAndHostWithDifferentPort() {
        // Arrange
        URI base = URI.create(wireMock.baseUrl());
        int differentPort = base.getPort() == 65535 ? 65534 : base.getPort() + 1;
        String eventUrl =
                "%s://%s:%d/bilety-v-teatr/buratino/".formatted(base.getScheme(), base.getHost(), differentPort);
        stubVenuePage(
                validVenuePage(eventJsonWithUrl(eventUrl, "Буратино", "2026-09-12T11:00:00+03:00", "2026-09-12")));

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl());

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("unexpected origin"))
                .verify();
    }

    @Test
    void shouldSendOnlyHtmlAcceptApplicationHeader() {
        // Arrange
        stubVenuePage(validVenuePage(eventJson("buratino", "Буратино", "2026-09-12T11:00:00+03:00")));

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl());

        // Assert
        StepVerifier.create(result).expectNextCount(1).verifyComplete();
        wireMock.verify(getRequestedFor(urlEqualTo(VENUE_PATH))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_HTML_VALUE))
                .withHeader(HttpHeaders.USER_AGENT, notMatching("LinkTracker/1[.]0"))
                .withoutHeader(HttpHeaders.ACCEPT_LANGUAGE));
    }

    @Test
    void shouldFailWhenVenueNamesDifferBetweenPages() {
        // Arrange
        stubVenuePage(
                1,
                """
                <html>
                  <head><link rel="last" href="%s?page=2"></head>
                  <body><h1>Площадка A</h1></body>
                </html>
                """
                        .formatted(VENUE_PATH));
        stubVenuePage(2, "<html><body><h1>Площадка B</h1></body></html>");

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl());

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().equals("Ticketpro venue name differs between pages"))
                .verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 404, 429, 500})
    void shouldPropagateTicketproHttpStatus(int status) {
        // Arrange
        wireMock.stubFor(get(urlEqualTo(VENUE_PATH)).willReturn(aResponse().withStatus(status)));

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl());

        // Assert
        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(WebClientResponseException.class);
                    assertThat(((WebClientResponseException) error)
                                    .getStatusCode()
                                    .value())
                            .isEqualTo(status);
                })
                .verify();
    }

    @Test
    void shouldTimeOutSlowTicketproResponse() {
        // Arrange
        wireMock.stubFor(get(urlEqualTo(VENUE_PATH))
                .willReturn(aResponse()
                        .withFixedDelay(16_000)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withBody("<html><body><h1>Площадка</h1></body></html>")));

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl());

        // Assert
        StepVerifier.create(result).expectError(TimeoutException.class).verify();
    }

    @Test
    void shouldRejectResponseLargerThanTwoMegabytes() {
        // Arrange
        wireMock.stubFor(get(urlEqualTo(VENUE_PATH))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withBody("x".repeat(2 * 1024 * 1024 + 1))));

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl());

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error -> hasCause(error, DataBufferLimitException.class))
                .verify();
    }

    @Test
    void shouldRejectVenueUrlFromUnexpectedSchemeOrHost() {
        // Arrange
        URI configuredBase = URI.create(wireMock.baseUrl());
        List<String> invalidUrls = List.of(
                "https://" + configuredBase.getHost() + ":" + configuredBase.getPort() + VENUE_PATH,
                configuredBase.getScheme() + "://example.com" + VENUE_PATH);

        // Act
        List<Mono<TicketproVenue>> results =
                invalidUrls.stream().map(client::getAvailableEvents).toList();

        // Assert
        results.forEach(result -> StepVerifier.create(result)
                .expectErrorMatches(error -> error instanceof IllegalArgumentException
                        && error.getMessage().contains("unexpected origin"))
                .verify());
        wireMock.verify(0, getRequestedFor(urlPathEqualTo(VENUE_PATH)));
    }

    @Test
    void shouldRejectVenueUrlFromDifferentPort() {
        // Arrange
        URI configuredBase = URI.create(wireMock.baseUrl());
        int differentPort = configuredBase.getPort() == 65_535 ? 65_534 : configuredBase.getPort() + 1;
        String venueUrl =
                configuredBase.getScheme() + "://" + configuredBase.getHost() + ":" + differentPort + VENUE_PATH;

        // Act
        Mono<TicketproVenue> result = client.getAvailableEvents(venueUrl);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error -> error instanceof IllegalArgumentException
                        && error.getMessage().contains("unexpected origin"))
                .verify();
        wireMock.verify(0, getRequestedFor(urlPathEqualTo(VENUE_PATH)));
    }

    @Test
    void shouldRejectVenueUrlWithUnsafeUriComponents() {
        // Arrange
        URI configuredBase = URI.create(wireMock.baseUrl());
        String origin = configuredBase.getScheme() + "://" + configuredBase.getHost() + ":" + configuredBase.getPort();
        List<String> invalidUrls = List.of(
                configuredBase.getScheme() + "://user@" + configuredBase.getHost() + ":" + configuredBase.getPort()
                        + VENUE_PATH,
                origin + VENUE_PATH + "?page=2",
                origin + VENUE_PATH + "#events",
                origin);

        // Act
        List<Mono<TicketproVenue>> results =
                invalidUrls.stream().map(client::getAvailableEvents).toList();

        // Assert
        results.forEach(result -> StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify());
        wireMock.verify(0, getRequestedFor(urlPathEqualTo(VENUE_PATH)));
    }

    @Test
    void shouldReturnImmutableVenueEvents() {
        // Arrange
        stubVenuePage("<html><body><h1>Площадка</h1></body></html>");

        // Act
        TicketproVenue result = client.getAvailableEvents(venueUrl()).block();

        // Assert
        assertThatThrownBy(() -> result.events().add(new TicketproEvent("A", "B", "C", "D", "E")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private String venueUrl() {
        return wireMock.baseUrl() + VENUE_PATH;
    }

    private void stubVenue(String path, String venueName) {
        wireMock.stubFor(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withBody("<html><body><h1>" + venueName + "</h1></body></html>")));
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> expectedType) {
        Throwable current = error;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
}
