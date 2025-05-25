package backend.academy.bot.scrapperclient;

import backend.academy.shared.dto.AddFilterRequest;
import backend.academy.shared.dto.AddLinkRequest;
import backend.academy.shared.dto.AddTagRequest;
import backend.academy.shared.dto.ApiErrorResponse;
import backend.academy.shared.dto.LinkResponse;
import backend.academy.shared.dto.ListLinksResponse;
import backend.academy.shared.dto.RegisterChatRequest;
import backend.academy.shared.dto.RemoveFilterRequest;
import backend.academy.shared.dto.RemoveLinkRequest;
import backend.academy.shared.dto.RemoveTagRequest;
import backend.academy.shared.dto.UpdateNotificationModeRequest;
import backend.academy.shared.exception.ApiException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ScrapperClient {
    private final WebClient scrapperClient;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;

    public ScrapperClient(
            @Value("${scrapper.base-url}") String baseUrl,
            Retry retry,
            CircuitBreaker circuitBreaker,
            TimeLimiter timeLimiter) {
        this.retry = retry;
        this.circuitBreaker = circuitBreaker;
        this.timeLimiter = timeLimiter;
        this.scrapperClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public Mono<Void> registerChat(Long chatId, RegisterChatRequest registerChatRequest) {
        return handleResponse(scrapperClient
                .post()
                .uri("/tg-chat/{id}", chatId)
                .body(BodyInserters.fromValue(registerChatRequest))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, this::processErrors)
                .bodyToMono(Void.class));
    }

    public Mono<Void> deleteChat(Long chatId) {
        return handleResponse(scrapperClient
                .delete()
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, this::processErrors)
                .onStatus(HttpStatus.NOT_FOUND::equals, this::processErrors)
                .bodyToMono(Void.class));
    }

    public Mono<ListLinksResponse> getTrackedLinks(Long chatId) {
        return handleResponse(scrapperClient
                .get()
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, this::processErrors)
                .bodyToMono(ListLinksResponse.class));
    }

    public Mono<LinkResponse> addTrackedLink(Long chatId, AddLinkRequest addLinkRequest) {
        return handleResponse(scrapperClient
                .post()
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(BodyInserters.fromValue(addLinkRequest))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, this::processErrors)
                .bodyToMono(LinkResponse.class));
    }

    public Mono<LinkResponse> deleteTrackedLink(Long chatId, RemoveLinkRequest removeLinkRequest) {
        return handleResponse(scrapperClient
                .method(HttpMethod.DELETE)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(BodyInserters.fromValue(removeLinkRequest))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, this::processErrors)
                .onStatus(HttpStatus.NOT_FOUND::equals, this::processErrors)
                .bodyToMono(LinkResponse.class));
    }

    public Mono<ListLinksResponse> getTrackedLinksByTag(Long chatId, String tag) {
        return handleResponse(scrapperClient
                .get()
                .uri("/links/by-tag?tag={tag}", tag)
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, this::processErrors)
                .bodyToMono(ListLinksResponse.class));
    }

    public Mono<Void> addTagToTrackedLink(Long chatId, AddTagRequest addTagRequest) {
        return handleResponse(scrapperClient
                .post()
                .uri("/tags/add")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(BodyInserters.fromValue(addTagRequest))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, this::processErrors)
                .onStatus(HttpStatus.NOT_FOUND::equals, this::processErrors)
                .bodyToMono(Void.class));
    }

    public Mono<Void> removeTagFromTrackedLink(Long chatId, RemoveTagRequest removeTagRequest) {
        return handleResponse(scrapperClient
                .method(HttpMethod.DELETE)
                .uri("/tags/remove")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(BodyInserters.fromValue(removeTagRequest))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, this::processErrors)
                .onStatus(HttpStatus.NOT_FOUND::equals, this::processErrors)
                .bodyToMono(Void.class));
    }

    public Mono<Void> addFilterToTrackedLink(Long chatId, AddFilterRequest addFilterRequest) {
        return handleResponse(scrapperClient
                .post()
                .uri("/filters/add")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(BodyInserters.fromValue(addFilterRequest))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, this::processErrors)
                .onStatus(HttpStatus.NOT_FOUND::equals, this::processErrors)
                .bodyToMono(Void.class));
    }

    public Mono<Void> removeFilterFromTrackedLink(Long chatId, RemoveFilterRequest removeFilterRequest) {
        return handleResponse(scrapperClient
                .method(HttpMethod.DELETE)
                .uri("/filters/remove")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(BodyInserters.fromValue(removeFilterRequest))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, this::processErrors)
                .onStatus(HttpStatus.NOT_FOUND::equals, this::processErrors)
                .bodyToMono(Void.class));
    }

    public Mono<Void> updateNotificationMode(Long chatId, UpdateNotificationModeRequest updateNotificationModeRequest) {
        return handleResponse(scrapperClient
                .patch()
                .uri("/tg-chat/{id}/notification", chatId)
                .body(BodyInserters.fromValue(updateNotificationModeRequest))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, this::processErrors)
                .bodyToMono(Void.class));
    }

    private <T> Mono<T> handleResponse(Mono<T> responseMono) {
        return responseMono
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter));
    }

    private Mono<? extends Throwable> processErrors(ClientResponse response) {
        return response.bodyToMono(ApiErrorResponse.class).flatMap(error -> Mono.error(new ApiException(error)));
    }
}
