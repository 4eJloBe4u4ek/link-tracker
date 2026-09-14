package backend.academy.scrapper.client.ticketpro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.scrapper.client.ticketpro.TicketproEventParser.VenuePage;
import backend.academy.scrapper.config.ScrapperConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TicketproEventParserTest {
    private static final String BASE_URL = "https://ticketpro.by";

    private TicketproEventParser parser;

    @BeforeEach
    void setUp() {
        ScrapperConfig.Ticketpro ticketpro = new ScrapperConfig.Ticketpro(BASE_URL);
        ScrapperConfig config = new ScrapperConfig(null, null, 100, null, null, null, ticketpro, null);
        parser = new TicketproEventParser(config, new ObjectMapper());
    }

    @Test
    void shouldParseEventsForAnyVenue() {
        // Arrange
        String html = venueHtml("ГУ Дворец Республики, Минск", eventJson("Граф Монте-Кристо", "ГУ Дворец Республики"));

        // Act
        VenuePage result = parser.parseVenuePage(html);

        // Assert
        assertThat(result.venueName()).isEqualTo("ГУ Дворец Республики, Минск");
        assertThat(result.events()).extracting(TicketproEvent::title).containsExactly("Граф Монте-Кристо");
    }

    @Test
    void shouldAcceptEmptyVenuePage() {
        // Arrange
        String html = "<html><body><h1>Белорусский государственный театр кукол, Минск</h1></body></html>";

        // Act
        VenuePage result = parser.parseVenuePage(html);

        // Assert
        assertThat(result.venueName()).isEqualTo("Белорусский государственный театр кукол, Минск");
        assertThat(result.events()).isEmpty();
        assertThat(result.lastPage()).isOne();
    }

    @ParameterizedTest
    @ValueSource(strings = {"<html><body></body></html>", "<html><body><h1>   </h1></body></html>"})
    void shouldRejectVenuePageWithoutHeading(String html) {
        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Ticketpro venue page has no heading");
    }

    @Test
    void shouldReadInStockEventsFromTicketproJsonLd() {
        // Arrange
        String eventUrl = BASE_URL + "/bilety-v-teatr/kot-v-sapogah/";
        String html =
                """
                <html>
                  <body>
                    <h1>Белорусский государственный театр кукол</h1>
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
                        .formatted(eventUrl);

        // Act
        VenuePage result = parser.parseVenuePage(html);

        // Assert
        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().title()).isEqualTo("Кот \"в сапогах\"");
        assertThat(result.events().getFirst().date()).isEqualTo("12.09.2026–13.09.2026");
        assertThat(result.events().getFirst().time()).isEqualTo("11:00");
        assertThat(result.events().getFirst().price()).isEqualTo("38–40 BYN");
        assertThat(result.events().getFirst().ticketUrl()).isEqualTo(eventUrl);
    }

    @Test
    void shouldReadJsonLdFromValidUnquotedScriptType() {
        // Arrange
        String html =
                """
                <html>
                  <body>
                    <h1>Белорусский государственный театр кукол</h1>
                    <script type=application/ld+json>%s</script>
                  </body>
                </html>
                """
                        .formatted(eventJson("buratino", "Буратино", "2026-09-12T11:00:00+03:00"));

        // Act
        VenuePage result = parser.parseVenuePage(html);

        // Assert
        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().title()).isEqualTo("Буратино");
    }

    @Test
    void shouldReadEventWithFullSchemaOrgType() {
        // Arrange
        String html = validVenuePage(eventJson(
                "buratino",
                "Буратино",
                "2026-09-12T11:00:00+03:00",
                "https://schema.org/Event",
                "https://schema.org/InStock"));

        // Act
        VenuePage result = parser.parseVenuePage(html);

        // Assert
        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().title()).isEqualTo("Буратино");
    }

    @Test
    void shouldIgnoreInStockValueFromAnotherVocabulary() {
        // Arrange
        String html = validVenuePage(
                eventJson("buratino", "Буратино", "2026-09-12T11:00:00+03:00", "Event", "https://example.com/InStock"));

        // Act
        VenuePage result = parser.parseVenuePage(html);

        // Assert
        assertThat(result.events()).isEmpty();
    }

    @Test
    void shouldIgnoreEventsThatAreNotInStock() {
        // Arrange
        String html =
                """
                <html>
                  <body>
                    <h1>Белорусский государственный театр кукол</h1>
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
                        .formatted(BASE_URL);

        // Act
        VenuePage result = parser.parseVenuePage(html);

        // Assert
        assertThat(result.events()).isEmpty();
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
        String html =
                """
        <html>
          <body>
            <h1>Белорусский государственный театр кукол</h1>
            %s
          </body>
        </html>
        """
                        .formatted(challengeFixture);

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("anti-bot challenge");
    }

    @Test
    void shouldFailWhenJsonLdIsMalformed() {
        // Arrange
        String html =
                """
                <html>
                  <body>
                    <h1>Белорусский государственный театр кукол</h1>
                    <script type="application/ld+json">{not-json}</script>
                  </body>
                </html>
                """;

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("malformed JSON-LD");
    }

    @Test
    void shouldAcceptUnescapedTabInJsonLdTextFromTicketpro() {
        // Arrange
        String jsonLd = eventJson("graf-monte-kristo", "Граф\tМонте-Кристо", "2026-09-12T19:00:00+03:00");
        String html = validVenuePage(jsonLd);

        // Act
        VenuePage result = parser.parseVenuePage(html);

        // Assert
        assertThat(result.events()).extracting(TicketproEvent::title).containsExactly("Граф\tМонте-Кристо");
    }

    @Test
    void shouldRejectTicketUrlFromAnotherOrigin() {
        // Arrange
        String anotherOrigin = "http://ticketpro.by/bilety-v-teatr/buratino/";
        String html =
                validVenuePage(eventJsonWithUrl(anotherOrigin, "Буратино", "2026-09-12T11:00:00+03:00", "2026-09-12"));

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unexpected origin");
    }

    @Test
    void shouldRejectTicketUrlFromDifferentPort() {
        // Arrange
        String differentPortUrl = "https://ticketpro.by:444/bilety-v-teatr/buratino/";
        String html = validVenuePage(
                eventJsonWithUrl(differentPortUrl, "Буратино", "2026-09-12T11:00:00+03:00", "2026-09-12"));

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unexpected origin");
    }

    @ParameterizedTest
    @CsvSource({
        "https://ticketpro.by, https://ticketpro.by:443/bilety-v-teatr/buratino/",
        "http://ticketpro.by, http://ticketpro.by:80/bilety-v-teatr/buratino/"
    })
    void shouldTreatExplicitDefaultPortAsConfiguredOrigin(String baseUrl, String eventUrl) {
        // Arrange
        TicketproEventParser defaultPortParser = parserFor(baseUrl);
        String html = validVenuePage(eventJsonWithUrl(eventUrl, "Буратино", "2026-09-12T11:00:00+03:00", "2026-09-12"));

        // Act
        VenuePage result = defaultPortParser.parseVenuePage(html);

        // Assert
        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().ticketUrl()).isEqualTo(eventUrl);
    }

    @Test
    void shouldFailWhenEndDatePrecedesStartDate() {
        // Arrange
        String html = validVenuePage(eventJsonWithUrl(
                BASE_URL + "/bilety-v-teatr/buratino/", "Буратино", "2026-09-13T11:00:00+03:00", "2026-09-12"));

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid date range");
    }

    @Test
    void shouldFailWhenEndTimePrecedesStartTimeOnTheSameDay() {
        // Arrange
        String html = validVenuePage(eventJsonWithUrl(
                BASE_URL + "/bilety-v-teatr/buratino/",
                "Буратино",
                "2026-09-12T18:00:00+03:00",
                "2026-09-12T17:00:00+03:00"));

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid date range");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-09-12T11:00:99+03:00", "2026-09-12T11:00:00+25:00", "2026-09-12T11:00:00+03:00junk"})
    void shouldRejectInvalidWholeStartDate(String startDate) {
        // Arrange
        String html = validVenuePage(
                eventJsonWithUrl(BASE_URL + "/bilety-v-teatr/buratino/", "Буратино", startDate, "2026-09-12"));

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid date");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-09-12T11:00:99+0300", "2026-09-12T11:00:00+2500", "2026-09-12T11:00:00+0300junk"})
    void shouldRejectInvalidWholeEndDate(String endDate) {
        // Arrange
        String html = validVenuePage(eventJsonWithUrl(
                BASE_URL + "/bilety-v-teatr/buratino/", "Буратино", "2026-09-12T11:00:00+03:00", endDate));

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid date");
    }

    @Test
    void shouldRejectPaginationAboveSafetyLimit() {
        // Arrange
        String html =
                """
                <html>
                  <head><link rel="last" href="%s?page=21"></head>
                  <body><h1>Белорусский государственный театр кукол</h1></body>
                </html>
                """
                        .formatted("/venue/ticketpro/");

        // Act
        ThrowingCallable action = () -> parser.parseVenuePage(html);

        // Assert
        assertThatThrownBy(action)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too many venue pages");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/venue/ticketpro/",
                "/venue/ticketpro/?page=0",
                "/venue/ticketpro/?page=-1",
                "/venue/ticketpro/?page=not-a-number"
            })
    void shouldRejectInvalidLastPageLink(String lastPageUrl) {
        // Arrange
        String html = """
                <html>
                  <head><link rel="last" href="%s"></head>
                  <body><h1>Белорусский государственный театр кукол</h1></body>
                </html>
                """
                .formatted(lastPageUrl);

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid venue pagination");
    }

    @Test
    void shouldAcceptRelativeTicketUrlFromConfiguredOrigin() {
        // Arrange
        String html = validVenuePage(
                eventJsonWithUrl("/bilety-v-teatr/buratino/", "Буратино", "2026-09-12T11:00:00+03:00", "2026-09-12"));

        // Act
        VenuePage result = parser.parseVenuePage(html);

        // Assert
        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().ticketUrl()).isEqualTo(BASE_URL + "/bilety-v-teatr/buratino/");
    }

    private String venueHtml(String venueName, String eventJson) {
        return "<html><body><h1>" + venueName
                + "</h1><script type=\"application/ld+json\">"
                + eventJson
                + "</script></body></html>";
    }

    private String eventJson(String title, String location) {
        return """
                {
                  "@type": "Event",
                  "name": "%s",
                  "startDate": "2026-08-26T19:00:00+0300",
                  "location": {"@type": "Place", "name": "%s"},
                  "url": "/bilety-v-teatr/graf-monte-kristo/",
                  "offers": {
                    "availability": "https://schema.org/InStock",
                    "lowPrice": "35",
                    "highPrice": "120",
                    "priceCurrency": "BYN"
                  }
                }
                """
                .formatted(title, location);
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
                .formatted(type, BASE_URL, slug, title, startDate, availability);
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

    private TicketproEventParser parserFor(String baseUrl) {
        ScrapperConfig.Ticketpro ticketpro = new ScrapperConfig.Ticketpro(baseUrl);
        ScrapperConfig config = new ScrapperConfig(null, null, 100, null, null, null, ticketpro, null);
        return new TicketproEventParser(config, new ObjectMapper());
    }
}
