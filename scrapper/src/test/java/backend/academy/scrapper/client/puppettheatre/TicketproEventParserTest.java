package backend.academy.scrapper.client.puppettheatre;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.scrapper.config.ScrapperConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TicketproEventParserTest {
    private TicketproEventParser parser;

    @BeforeEach
    void setUp() {
        ScrapperConfig.PuppetTheatre properties =
                new ScrapperConfig.PuppetTheatre("https://www.ticketpro.by", "/venue/");
        ScrapperConfig config = new ScrapperConfig(null, null, 100, null, null, null, properties, null);
        parser = new TicketproEventParser(config, new ObjectMapper());
    }

    @Test
    void shouldAcceptValidEmptyVenuePage() {
        // Arrange
        String html = venuePage("", "");

        // Act
        TicketproEventParser.VenuePage page = parser.parseVenuePage(html);

        // Assert
        assertThat(page.sessions()).isEmpty();
        assertThat(page.lastPage()).isEqualTo(1);
    }

    @Test
    void shouldAcceptMatchingHtmlCardAndSoldOutJsonLdEvent() {
        // Arrange
        String html = venuePage("<div class=\"event-box\"></div>", eventJson("https://schema.org/SoldOut"));

        // Act
        TicketproEventParser.VenuePage page = parser.parseVenuePage(html);

        // Assert
        assertThat(page.sessions()).isEmpty();
    }

    @Test
    void shouldRejectHtmlCardWithoutJsonLdEvent() {
        // Arrange
        String html = venuePage("<div class=\"event-box\"></div>", "");

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOfSatisfying(TicketproException.class, exception ->
                        assertThat(exception.result()).isEqualTo(TicketproCheckResult.PARSE_ERROR));
    }

    @Test
    void shouldRejectJsonLdEventWithoutHtmlCard() {
        // Arrange
        String html = venuePage("", eventJson("https://schema.org/InStock"));

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOfSatisfying(TicketproException.class, exception ->
                        assertThat(exception.result()).isEqualTo(TicketproCheckResult.PARSE_ERROR));
    }

    @Test
    void shouldRejectVenueEventWithUnexpectedLocation() {
        // Arrange
        String jsonLd = eventJson("https://schema.org/InStock")
                .replace("Белорусский государственный театр кукол", "Другая площадка");
        String html = venuePage("<div class=\"event-box\"></div>", jsonLd);

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOfSatisfying(TicketproException.class, exception -> {
                    assertThat(exception.result()).isEqualTo(TicketproCheckResult.PARSE_ERROR);
                    assertThat(exception).hasMessageContaining("unexpected venue");
                });
    }

    @Test
    void shouldClassifyAntiBotChallenge() {
        // Arrange
        String html = venuePage("<div>Protected by Anubis</div>", "");

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOfSatisfying(TicketproException.class, exception ->
                        assertThat(exception.result()).isEqualTo(TicketproCheckResult.ANTIBOT));
    }

    @Test
    void shouldNotTreatEventNamedAnubisAsAntiBotChallenge() {
        // Arrange
        String jsonLd = eventJson("https://schema.org/InStock").replace("Буратино", "Anubis");
        String html = venuePage("<div class=\"event-box\"></div>", jsonLd);

        // Act
        TicketproEventParser.VenuePage page = parser.parseVenuePage(html);

        // Assert
        assertThat(page.sessions()).singleElement().extracting(PuppetTheatreSession::title).isEqualTo("Anubis");
    }

    @Test
    void shouldClassifyUnexpectedVenuePageAsInvalidContent() {
        // Arrange
        String html = "<html><body><h1>Другая площадка</h1></body></html>";

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOfSatisfying(TicketproException.class, exception ->
                        assertThat(exception.result()).isEqualTo(TicketproCheckResult.INVALID_CONTENT));
    }

    @Test
    void shouldRejectPageWithVenueNameOutsideMainHeading() {
        // Arrange
        String html = """
                <html>
                  <head><title>Белорусский государственный театр кукол</title></head>
                  <body><p>Временная служебная страница</p></body>
                </html>
                """;

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOfSatisfying(TicketproException.class, exception ->
                        assertThat(exception.result()).isEqualTo(TicketproCheckResult.INVALID_CONTENT));
    }

    @Test
    void shouldClassifyMalformedJsonLdAsParseError() {
        // Arrange
        String html = venuePage("", "{not-json}");

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOfSatisfying(TicketproException.class, exception ->
                        assertThat(exception.result()).isEqualTo(TicketproCheckResult.PARSE_ERROR));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectNonPositiveLastPage(int lastPage) {
        // Arrange
        String html = venuePage("<link rel=\"last\" href=\"/venue/?page=" + lastPage + "\">", "");

        // Act & Assert
        assertThatThrownBy(() -> parser.parseVenuePage(html))
                .isInstanceOfSatisfying(TicketproException.class, exception -> {
                    assertThat(exception.result()).isEqualTo(TicketproCheckResult.PARSE_ERROR);
                    assertThat(exception).hasMessageContaining("invalid venue page count");
                });
    }

    private String venuePage(String cards, String jsonLd) {
        String script = jsonLd.isBlank() ? "" : "<script type=\"application/ld+json\">" + jsonLd + "</script>";
        return """
        <html>
          <body>
            <h1>Белорусский государственный театр кукол</h1>
            %s
            %s
          </body>
        </html>
        """
                .formatted(cards, script);
    }

    private String eventJson(String availability) {
        return """
        {
          "@type": "Event",
          "url": "/bilety-v-teatr/buratino/",
          "name": "Буратино",
          "startDate": "2026-09-12T11:00:00+03:00",
          "location": {"name": "Белорусский государственный театр кукол"},
          "offers": {"availability": "%s"}
        }
        """
                .formatted(availability);
    }
}
