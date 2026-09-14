package backend.academy.scrapper.config.bucket;

import static org.assertj.core.api.Assertions.assertThat;

import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisURI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class RedisPropertiesTest {
    private static final String SECURE_REDIS_URL = "rediss://app:secret@redis.example.com:27246";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(RedisPropertiesConfiguration.class);

    @Test
    void shouldBindSecureRedisUrl() {
        // Arrange
        ApplicationContextRunner secureUrlContextRunner =
                contextRunner.withPropertyValues("spring.data.redis.url=" + SECURE_REDIS_URL);

        // Act
        secureUrlContextRunner.run(context -> {
            // Assert
            assertThat(context).hasNotFailed();
            RedisURI redisUri = context.getBean(RedisProperties.class).redisUri();
            assertThat(redisUri.getHost()).isEqualTo("redis.example.com");
            assertThat(redisUri.getPort()).isEqualTo(27246);
            assertThat(redisUri.isSsl()).isTrue();
            RedisCredentials credentials = redisUri.getCredentialsProvider()
                    .resolveCredentials()
                    .block();
            assertThat(credentials).isNotNull();
            assertThat(credentials.getUsername()).isEqualTo("app");
            assertThat(credentials.getPassword()).containsExactly("secret".toCharArray());
        });
    }

    @Test
    void shouldFallbackToHostAndPortWhenUrlIsNotConfigured() {
        // Arrange
        ApplicationContextRunner hostAndPortContextRunner =
                contextRunner.withPropertyValues("spring.data.redis.host=redis", "spring.data.redis.port=6380");

        // Act
        hostAndPortContextRunner.run(context -> {
            // Assert
            assertThat(context).hasNotFailed();
            RedisURI redisUri = context.getBean(RedisProperties.class).redisUri();
            assertThat(redisUri.getHost()).isEqualTo("redis");
            assertThat(redisUri.getPort()).isEqualTo(6380);
            assertThat(redisUri.isSsl()).isFalse();
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RedisProperties.class)
    static class RedisPropertiesConfiguration {}
}
