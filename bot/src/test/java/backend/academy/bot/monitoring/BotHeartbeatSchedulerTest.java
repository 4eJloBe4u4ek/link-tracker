package backend.academy.bot.monitoring;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.config.MonitoringProperties;
import backend.academy.bot.telegrambot.BotService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class BotHeartbeatSchedulerTest {
    @Test
    void shouldPingWhenTelegramApiAndLongPollingAreHealthy() {
        // Arrange
        BotService botService = mock(BotService.class);
        HealthchecksClient healthchecksClient = mock(HealthchecksClient.class);
        when(healthchecksClient.isEnabled()).thenReturn(true);
        when(botService.hasRecentPollingError(any(Duration.class))).thenReturn(false);
        when(botService.telegramApiAvailable()).thenReturn(true);
        BotHeartbeatScheduler scheduler = new BotHeartbeatScheduler(botService, healthchecksClient);

        // Act
        scheduler.checkBot();

        // Assert
        verify(healthchecksClient).pingBot();
    }

    @Test
    void shouldNotPingWhenTelegramApiIsUnavailable() {
        // Arrange
        BotService botService = mock(BotService.class);
        HealthchecksClient healthchecksClient = mock(HealthchecksClient.class);
        when(healthchecksClient.isEnabled()).thenReturn(true);
        when(botService.hasRecentPollingError(any(Duration.class))).thenReturn(false);
        when(botService.telegramApiAvailable()).thenReturn(false);
        BotHeartbeatScheduler scheduler = new BotHeartbeatScheduler(botService, healthchecksClient);

        // Act
        scheduler.checkBot();

        // Assert
        verify(healthchecksClient, never()).pingBot();
    }

    @Test
    void shouldNotPingWhenLongPollingErrorIsRecent() {
        // Arrange
        BotService botService = mock(BotService.class);
        HealthchecksClient healthchecksClient = mock(HealthchecksClient.class);
        when(healthchecksClient.isEnabled()).thenReturn(true);
        when(botService.hasRecentPollingError(any(Duration.class))).thenReturn(true);
        BotHeartbeatScheduler scheduler = new BotHeartbeatScheduler(botService, healthchecksClient);

        // Act
        scheduler.checkBot();

        // Assert
        verify(botService, never()).telegramApiAvailable();
        verify(healthchecksClient, never()).pingBot();
    }

    @Test
    void shouldNotCallTelegramApiWhenBotMonitoringIsDisabled() {
        // Arrange
        BotService botService = mock(BotService.class);
        HealthchecksClient healthchecksClient = new HealthchecksClient(
                new MonitoringProperties(""),
                WebClient.builder(),
                new SimpleMeterRegistry(),
                Clock.systemUTC());
        BotHeartbeatScheduler scheduler = new BotHeartbeatScheduler(botService, healthchecksClient);

        // Act
        scheduler.checkBot();

        // Assert
        verify(botService, never()).telegramApiAvailable();
    }
}
