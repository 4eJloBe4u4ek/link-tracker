package backend.academy.bot.telegrambot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.command.CommandHandler;
import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.TelegramException;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.response.GetMeResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BotServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-10T15:00:00Z");

    @Test
    void shouldRememberRecentLongPollingError() {
        // Arrange
        TelegramBot bot = mock(TelegramBot.class);
        CommandHandler commandHandler = mock(CommandHandler.class);
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW);
        BotService service = new BotService(bot, commandHandler, clock);
        service.start();
        ArgumentCaptor<ExceptionHandler> exceptionHandlerCaptor = ArgumentCaptor.forClass(ExceptionHandler.class);
        verify(bot).setUpdatesListener(any(UpdatesListener.class), exceptionHandlerCaptor.capture());

        // Act
        exceptionHandlerCaptor
                .getValue()
                .onException(new TelegramException(new IOException("telegram unavailable")));

        // Assert
        assertThat(service.hasRecentPollingError(Duration.ofMinutes(2))).isTrue();
    }

    @Test
    void shouldForgetLongPollingErrorAfterRecoveryWindow() {
        // Arrange
        TelegramBot bot = mock(TelegramBot.class);
        CommandHandler commandHandler = mock(CommandHandler.class);
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW, NOW.plusSeconds(121));
        BotService service = new BotService(bot, commandHandler, clock);
        service.start();
        ArgumentCaptor<ExceptionHandler> exceptionHandlerCaptor = ArgumentCaptor.forClass(ExceptionHandler.class);
        verify(bot).setUpdatesListener(any(UpdatesListener.class), exceptionHandlerCaptor.capture());
        exceptionHandlerCaptor
                .getValue()
                .onException(new TelegramException(new IOException("telegram unavailable")));

        // Act
        boolean recentError = service.hasRecentPollingError(Duration.ofMinutes(2));

        // Assert
        assertThat(recentError).isFalse();
    }

    @Test
    void shouldReportTelegramApiAvailabilityFromGetMe() {
        // Arrange
        TelegramBot bot = mock(TelegramBot.class);
        GetMeResponse response = mock(GetMeResponse.class);
        when(response.isOk()).thenReturn(true);
        when(bot.execute(any(GetMe.class))).thenReturn(response);
        BotService service = new BotService(bot, mock(CommandHandler.class), Clock.systemUTC());

        // Act
        boolean available = service.telegramApiAvailable();

        // Assert
        assertThat(available).isTrue();
    }

    @Test
    void shouldReportTelegramApiUnavailableWhenGetMeFails() {
        // Arrange
        TelegramBot bot = mock(TelegramBot.class);
        when(bot.execute(any(GetMe.class))).thenThrow(new IllegalStateException("telegram unavailable"));
        BotService service = new BotService(bot, mock(CommandHandler.class), Clock.systemUTC());

        // Act
        boolean available = service.telegramApiAvailable();

        // Assert
        assertThat(available).isFalse();
    }
}
