package backend.academy.scrapper.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record ScrapperConfig(
        AccessType accessType,
        @Positive int batchSize,
        @Bean Scheduler scheduler,
        GithubCredentials github,
        StackOverflowCredentials stackOverflow) {

    public enum AccessType {
        SQL,
        ORM
    }

    public record Scheduler(@Positive int interval, @Positive int threadCount) {}

    public record GithubCredentials(@NotEmpty String token, String baseUrl) {
        public GithubCredentials {
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://api.github.com";
            }
        }
    }

    public record StackOverflowCredentials(@NotEmpty String key, @NotEmpty String accessToken, String baseUrl) {
        public StackOverflowCredentials {
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://api.stackexchange.com/2.3";
            }
        }
    }
}
