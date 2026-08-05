package backend.academy.bot.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class UrlCheckerTest {
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void shouldValidateCorrectUrls() {
        // Arrange
        wireMock.stubFor(head(urlEqualTo("/first")).willReturn(aResponse().withStatus(200)));
        wireMock.stubFor(head(urlEqualTo("/second")).willReturn(aResponse().withStatus(200)));
        String validUrl1 = wireMock.baseUrl() + "/first";
        String validUrl2 = wireMock.baseUrl() + "/second";

        // Act & Assert
        Assertions.assertTrue(UrlChecker.isValidUrl(validUrl1));
        Assertions.assertTrue(UrlChecker.isValidUrl(validUrl2));
    }

    @Test
    void shouldInvalidateIncorrectUrls() {
        // Arrange
        String invalidUrl1 = "htt://invalid-url";
        String invalidUrl2 = "ftp://unsupported-protocol.com";
        String invalidUrl3 = "random text";

        // Act & Assert
        Assertions.assertFalse(UrlChecker.isValidUrl(invalidUrl1));
        Assertions.assertFalse(UrlChecker.isValidUrl(invalidUrl2));
        Assertions.assertFalse(UrlChecker.isValidUrl(invalidUrl3));
    }
}
