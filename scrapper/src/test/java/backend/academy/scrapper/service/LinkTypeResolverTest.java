package backend.academy.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.shared.dto.LinkType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LinkTypeResolverTest {
    private final LinkTypeResolver resolver = new LinkTypeResolver();

    @ParameterizedTest
    @CsvSource({
        "https://github.com/spring-projects/spring-boot, GITHUB",
        "https://stackoverflow.com/questions/12345/example-question, STACKOVERFLOW",
        "https://puppet-minsk.by/afisha, PUPPET_THEATRE"
    })
    void shouldResolveSupportedLinkType(String url, LinkType expectedType) {
        // Act & Assert
        assertThat(resolver.resolve(url)).contains(expectedType);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://github.com/openai",
                "https://example.com/",
                "https://puppet-minsk.by/afisha/",
                "https://www.puppet-minsk.by/afisha",
                "http://puppet-minsk.by/afisha",
                "https://puppet-minsk.by/afisha?date=2026-09-12"
            })
    void shouldRejectUnsupportedLinks(String url) {
        // Act & Assert
        assertThat(resolver.resolve(url)).isEmpty();
    }
}
