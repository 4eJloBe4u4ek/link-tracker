package backend.academy.scrapper.config.bucket;

import io.lettuce.core.RedisURI;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.data.redis", ignoreUnknownFields = false)
public record RedisProperties(int port, String host, URI url) {
    public RedisURI redisUri() {
        if (url != null) {
            return RedisURI.create(url.toString());
        }
        return RedisURI.builder().withHost(host).withPort(port).build();
    }
}
