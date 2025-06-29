package backend.academy.bot.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UrlCheckerTest {
    @Test
    void shouldValidateCorrectUrls() {
        Assertions.assertTrue(UrlChecker.isValidUrl("https://github.com/pengrad/java-telegram-bot-api"));
        Assertions.assertTrue(UrlChecker.isValidUrl("https://stackoverflow.com/questions"));
    }

    @Test
    void shouldInvalidateIncorrectUrls() {
        Assertions.assertFalse(UrlChecker.isValidUrl("htt://invalid-url"));
        Assertions.assertFalse(UrlChecker.isValidUrl("ftp://unsupported-protocol.com"));
        Assertions.assertFalse(UrlChecker.isValidUrl("random text"));
    }
}
