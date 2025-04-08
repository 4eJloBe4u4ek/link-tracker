package backend.academy.bot.config;

import com.pengrad.telegrambot.TelegramBot;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record BotConfig(@NotEmpty String telegramToken, UpdateEvents updateEvents) {
    @Bean
    public TelegramBot telegramBot() {
        return new TelegramBot(telegramToken);
    }

    public record UpdateEvents(
            @NotEmpty String topic,
            @NotEmpty String dlqTopic,
            @NotEmpty String consumerGroupId,
            @Positive int concurrency,
            @NotEmpty String trustedPackages) {}
}
