package backend.academy.scrapper.botclient;

import backend.academy.shared.dto.ApiErrorResponse;
import backend.academy.shared.dto.LinkUpdate;
import backend.academy.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class BotClient {
    private static final String BASE_URL = "http://localhost:8080";
    private final WebClient botClient;

    public BotClient() {
        this.botClient = WebClient.builder().baseUrl(BASE_URL).build();
    }

    public Mono<Void> updateLink(LinkUpdate update) {
        return botClient
                .post()
                .uri("/updates")
                .body(BodyInserters.fromValue(update))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> response.bodyToMono(ApiErrorResponse.class)
                        .flatMap(error -> Mono.error(new ApiException(error))))
                .bodyToMono(Void.class);
    }
}
