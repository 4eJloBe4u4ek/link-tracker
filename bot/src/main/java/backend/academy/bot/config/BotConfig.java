package backend.academy.bot.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record BotConfig(@NotEmpty String telegramToken, UpdateEvents updateEvents) {
    public record UpdateEvents(
            @NotEmpty String topic,
            @NotEmpty String dlqTopic,
            @NotEmpty String consumerGroupId,
            @Positive int concurrency,
            @NotEmpty String trustedPackages) {}
}
