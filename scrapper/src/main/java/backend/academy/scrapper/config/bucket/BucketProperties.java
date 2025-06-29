package backend.academy.scrapper.config.bucket;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bucket", ignoreUnknownFields = false)
public record BucketProperties(long capacity, Duration duration) {}
