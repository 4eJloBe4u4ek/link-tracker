package backend.academy.bot.scrapperclient;

import backend.academy.shared.dto.AddLinkRequest;
import backend.academy.shared.dto.AddTagRequest;
import backend.academy.shared.dto.ApiErrorResponse;
import backend.academy.shared.dto.LinkResponse;
import backend.academy.shared.dto.ListLinksResponse;
import backend.academy.shared.dto.RemoveLinkRequest;
import backend.academy.shared.dto.RemoveTagRequest;
import backend.academy.shared.exception.ApiException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ScrapperClient {
    private static final String BASE_URL = "http://localhost:8081";
    private final WebClient scrapperClient;

    public ScrapperClient() {
        this.scrapperClient = WebClient.builder().baseUrl(BASE_URL).build();
    }

    public Mono<Void> registerChat(Long chatId) {
        return scrapperClient
                .post()
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .bodyToMono(Void.class);
    }

    public Mono<Void> deleteChat(Long chatId) {
        return scrapperClient
                .delete()
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .onStatus(HttpStatus.NOT_FOUND::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .bodyToMono(Void.class);
    }

    public Mono<ListLinksResponse> getTrackedLinks(Long chatId) {
        return scrapperClient
                .get()
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .bodyToMono(ListLinksResponse.class);
    }

    public Mono<LinkResponse> addTrackedLink(Long chatId, AddLinkRequest addLinkRequest) {
        return scrapperClient
                .post()
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(BodyInserters.fromValue(addLinkRequest))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .bodyToMono(LinkResponse.class);
    }

    public Mono<LinkResponse> deleteTrackedLink(Long chatId, RemoveLinkRequest removeLinkRequest) {
        return scrapperClient
                .method(HttpMethod.DELETE)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(BodyInserters.fromValue(removeLinkRequest))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .onStatus(HttpStatus.NOT_FOUND::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .bodyToMono(LinkResponse.class);
    }

    public Mono<ListLinksResponse> getTrackedLinksByTag(Long chatId, String tag) {
        // todo status codes
        return scrapperClient
                .get()
                .uri("/links/by-tag?tag={tag}", tag)
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .bodyToMono(ListLinksResponse.class);
    }

    public Mono<Void> addTagToTrackedLink(Long chatId, AddTagRequest addTagRequest) {
        // todo status codes
        return scrapperClient
                .post()
                .uri("/tags/add")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(BodyInserters.fromValue(addTagRequest))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .onStatus(HttpStatus.NOT_FOUND::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .bodyToMono(Void.class);
    }

    public Mono<Void> removeTagFromTrackedLink(Long chatId, RemoveTagRequest removeTagRequest) {
        // todo status codes
        return scrapperClient
                .method(HttpMethod.DELETE)
                .uri("/tags/remove")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .body(BodyInserters.fromValue(removeTagRequest))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .onStatus(HttpStatus.NOT_FOUND::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .bodyToMono(Void.class);
    }
}
