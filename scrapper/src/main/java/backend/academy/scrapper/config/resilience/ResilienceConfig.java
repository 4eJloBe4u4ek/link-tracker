package backend.academy.scrapper.config.resilience;

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
    public Retry botRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("botClient");
    }

    @Bean
    public CircuitBreaker botCircuitBreaker(CircuitBreakerRegistry cbRegistry) {
        return cbRegistry.circuitBreaker("botClient");
    }

    @Bean
    public TimeLimiter botTimeLimiter(TimeLimiterRegistry timeLimiterRegistry) {
        return timeLimiterRegistry.timeLimiter("botClient");
    }
}
