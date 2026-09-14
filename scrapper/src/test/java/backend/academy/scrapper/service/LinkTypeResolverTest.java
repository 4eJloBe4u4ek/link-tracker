package backend.academy.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.shared.dto.LinkType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LinkTypeResolverTest {
    private static final int MAX_CANONICAL_TICKETPRO_VENUE_URL_LENGTH = 255;
    private static final String TICKETPRO_VENUE_URL_PREFIX = "https://www.ticketpro.by/koncertnye-ploshhadki/";
    private final LinkTypeResolver resolver = new LinkTypeResolver();

    @Test
    void shouldReturnEmptyForNullUrl() {
        // Act & Assert
        assertThat(resolver.resolve(null)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "https://github.com/spring-projects/spring-boot, GITHUB",
        "https://stackoverflow.com/questions/12345/example-question, STACKOVERFLOW"
    })
    void shouldResolveSupportedLinkType(String url, LinkType expectedType) {
        // Act & Assert
        assertThat(resolver.resolve(url)).contains(expectedType);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://www.ticketpro.by/koncertnye-ploshhadki/belorusskij-gosudarstvennyj-teatr-kukol/",
                "https://www.ticketpro.by/koncertnye-ploshhadki/dvorec-respubliki/",
                "https://www.ticketpro.by/koncertnye-ploshhadki/kz-minsk/"
            })
    void shouldResolveCanonicalTicketproVenueLink(String url) {
        // Act & Assert
        assertThat(resolver.resolve(url)).contains(LinkType.TICKETPRO);
    }

    @Test
    void shouldResolveTicketproVenueLinkAtMaximumLength() {
        // Arrange
        String url = canonicalVenueUrl(MAX_CANONICAL_TICKETPRO_VENUE_URL_LENGTH);

        // Act & Assert
        assertThat(resolver.resolve(url)).contains(LinkType.TICKETPRO);
    }

    @Test
    void shouldRejectTicketproVenueLinkOverMaximumLength() {
        // Arrange
        String url = canonicalVenueUrl(MAX_CANONICAL_TICKETPRO_VENUE_URL_LENGTH + 1);

        // Act & Assert
        assertThat(resolver.resolve(url)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://github.com/openai",
                "https://example.com/",
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
    void shouldRejectUnsupportedLinks(String url) {
        // Act & Assert
        assertThat(resolver.resolve(url)).isEmpty();
    }

    private static String canonicalVenueUrl(int length) {
        return TICKETPRO_VENUE_URL_PREFIX + "a".repeat(length - TICKETPRO_VENUE_URL_PREFIX.length() - 1) + "/";
    }
}
