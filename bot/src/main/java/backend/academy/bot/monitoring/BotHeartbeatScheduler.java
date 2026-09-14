package backend.academy.bot.monitoring;

import backend.academy.bot.telegrambot.BotService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotHeartbeatScheduler {
    private static final Duration POLLING_ERROR_WINDOW = Duration.ofMinutes(2);

    private final BotService botService;
    private final HealthchecksClient healthchecksClient;

    @Scheduled(fixedDelay = 60_000)
    public void checkBot() {
        if (healthchecksClient.isEnabled()
                && !botService.hasRecentPollingError(POLLING_ERROR_WINDOW)
                && botService.telegramApiAvailable()) {
            healthchecksClient.pingBot();
        }
    }
}
