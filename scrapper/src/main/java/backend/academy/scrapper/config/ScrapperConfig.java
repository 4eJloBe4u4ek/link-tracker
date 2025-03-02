package backend.academy.scrapper.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record ScrapperConfig(
        @NotEmpty String githubToken, String githubBaseUrl, StackOverflowCredentials stackOverflow) {

    public ScrapperConfig {
        if (githubBaseUrl == null || githubBaseUrl.isBlank()) {
            githubBaseUrl = "https://api.github.com";
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
