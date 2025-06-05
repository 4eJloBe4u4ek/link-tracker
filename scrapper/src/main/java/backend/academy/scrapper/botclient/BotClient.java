package backend.academy.scrapper.botclient;

import backend.academy.shared.api.ApiEndpoints;
import backend.academy.shared.dto.ApiErrorResponse;
import backend.academy.shared.dto.LinkUpdate;
import backend.academy.shared.exception.ApiException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class BotClient {
    private final WebClient botClient;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;

    public BotClient(
            @Value("${bot.base-url}") String baseUrl,
            Retry retry,
            CircuitBreaker circuitBreaker,
            TimeLimiter timeLimiter) {
        this.retry = retry;
        this.circuitBreaker = circuitBreaker;
        this.timeLimiter = timeLimiter;
        this.botClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public Mono<Void> updateLink(LinkUpdate update) {
        return botClient
                .post()
                .uri(ApiEndpoints.UPDATES)
                .body(BodyInserters.fromValue(update))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .bodyToMono(Void.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter));
    }
}
