package backend.academy.bot.config.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

    @Bean
    public Retry scrapperRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("scrapperClient");
    }

    @Bean
    public CircuitBreaker scrapperCircuitBreaker(CircuitBreakerRegistry cbRegistry) {
        return cbRegistry.circuitBreaker("scrapperClient");
    }

    @Bean
    public TimeLimiter scrapperTimeLimiter(TimeLimiterRegistry timeLimiterRegistry) {
        return timeLimiterRegistry.timeLimiter("scrapperClient");
    }
}
