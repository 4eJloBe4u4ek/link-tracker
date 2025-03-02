package backend.academy.scrapper.client.stackoverflow;

import backend.academy.scrapper.config.ScrapperConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class StackoverflowClient {
    private final WebClient stackOverflowClient;
    private final String apiKey;

    public StackoverflowClient(ScrapperConfig scrapperConfig) {
        this.apiKey = scrapperConfig.stackOverflow().key();
        this.stackOverflowClient = WebClient.builder()
                .baseUrl(scrapperConfig.stackOverflow().baseUrl())
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        scrapperConfig.stackOverflow().accessToken())
                .build();
    }

    public Mono<StackoverflowQuestion> getQuestion(Long questionId) {
        return stackOverflowClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/questions/{ids}")
                        .queryParam("key", apiKey)
                        .queryParam("site", "stackoverflow")
                        .queryParam("sort", "activity")
                        .queryParam("order", "desc")
                        .build(questionId))
                .retrieve()
                .bodyToMono(StackoverflowResponse.class)
                .map(response -> response.questions().getFirst());
    }
}
