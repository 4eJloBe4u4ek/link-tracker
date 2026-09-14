package backend.academy.bot;

import backend.academy.bot.config.BotConfig;
import backend.academy.bot.config.MonitoringProperties;
import backend.academy.bot.config.bucket.BucketProperties;
import backend.academy.bot.config.bucket.RedisProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties({
    BotConfig.class,
    MonitoringProperties.class,
    BucketProperties.class,
    RedisProperties.class
})
@ComponentScan(basePackages = {"backend.academy"})
public class BotApplication {
    public static void main(String[] args) {
        SpringApplication.run(BotApplication.class, args);
    }
}
