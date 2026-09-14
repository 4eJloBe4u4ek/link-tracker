package backend.academy.scrapper.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record ScrapperConfig(
        AccessType accessType,
        MessageTransport messageTransport,
        @Positive int batchSize,
        @Bean Scheduler scheduler,
        GithubCredentials github,
        StackOverflowCredentials stackOverflow,
        @Valid @NotNull Ticketpro ticketpro,
        UpdateEvents updateEvents) {

    public enum AccessType {
        SQL,
        ORM
    }

    public enum MessageTransport {
        HTTP,
        KAFKA
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

    public record Ticketpro(@NotEmpty String baseUrl) {}

    public record UpdateEvents(
            @NotEmpty String topic, @NotEmpty String dlqTopic, @Positive int partitions, @Positive short replicas) {
        public KafkaAdmin.NewTopics toNewTopics() {
            return new KafkaAdmin.NewTopics(
                    new NewTopic(topic, partitions, replicas), new NewTopic(dlqTopic, partitions, replicas));
        }
    }
}
