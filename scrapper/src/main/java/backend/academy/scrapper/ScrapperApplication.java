package backend.academy.scrapper;

import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.scrapper.config.MonitoringProperties;
import backend.academy.scrapper.config.bucket.BucketProperties;
import backend.academy.scrapper.config.bucket.RedisProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
    ScrapperConfig.class,
    MonitoringProperties.class,
    BucketProperties.class,
    RedisProperties.class
})
public class ScrapperApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScrapperApplication.class, args);
    }
}
