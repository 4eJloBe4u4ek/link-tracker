package backend.academy.bot.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UrlCheckerTest {
    @Test
    void shouldValidateCorrectUrls() {
        // Arrange
        String validUrl1 = "https://github.com/pengrad/java-telegram-bot-api";
        String validUrl2 = "https://stackoverflow.com/questions";

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
