package backend.academy.scrapper.config.bucket;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.data.redis", ignoreUnknownFields = false)
public record RedisProperties(int port, String host) {}
