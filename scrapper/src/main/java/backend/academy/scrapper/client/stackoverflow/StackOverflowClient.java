package backend.academy.scrapper.client.stackoverflow;

import backend.academy.scrapper.config.ScrapperConfig;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class StackOverflowClient {
    private final WebClient stackOverflowClient;
    private final String apiKey;

    public StackOverflowClient(ScrapperConfig scrapperConfig) {
        this.apiKey = scrapperConfig.stackOverflow().key();
        this.stackOverflowClient = WebClient.builder()
                .baseUrl(scrapperConfig.stackOverflow().baseUrl())
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        scrapperConfig.stackOverflow().accessToken())
                .build();
    }

    public Mono<StackOverflowQuestionUpdates> getStackoverflowUpdates(Long questionId, LocalDateTime lastUpdatedAt) {
        Instant instant = lastUpdatedAt.atZone(ZoneId.systemDefault()).toInstant();
        Long min = instant.getEpochSecond();

        Mono<StackOverflowQuestion> questionMono = getQuestion(questionId);
        Mono<List<StackOverflowAnswer>> answersMono = getAnswers(questionId, min);
        Mono<List<StackOverflowComment>> commentsToQuestionMono = getCommentsToQuestion(questionId, min);
        Mono<List<StackOverflowComment>> commentsToAnswerMono = answersMono.flatMap(answers -> {
            List<Long> answersIds =
                    answers.stream().map(StackOverflowAnswer::answerId).toList();

            return getCommentsToAnswer(answersIds, min);
        });

        return Mono.zip(questionMono, answersMono, commentsToQuestionMono, commentsToAnswerMono)
                .map(tuple ->
                        new StackOverflowQuestionUpdates(tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()));
    }

    public Mono<StackOverflowQuestion> getQuestion(Long questionId) {
        return stackOverflowClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/questions/{ids}")
                        .queryParam("key", apiKey)
                        .queryParam("site", "stackoverflow")
                        .build(questionId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<StackOverflowResponse<StackOverflowQuestion>>() {})
                .flatMap(response -> response.items().isEmpty()
                        ? Mono.empty()
                        : Mono.just(response.items().getFirst()));
    }

    public Mono<List<StackOverflowAnswer>> getAnswers(Long questionId, Long min) {
        return stackOverflowClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/questions/{ids}/answers")
                        .queryParam("key", apiKey)
                        .queryParam("site", "stackoverflow")
                        .queryParam("sort", "creation")
                        .queryParam("order", "desc")
                        .queryParam("min", min)
                        .queryParam("filter", "withbody")
                        .build(questionId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<StackOverflowResponse<StackOverflowAnswer>>() {})
                .map(StackOverflowResponse::items);
    }

    public Mono<List<StackOverflowComment>> getCommentsToQuestion(Long questionId, Long min) {
        return stackOverflowClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/questions/{ids}/comments")
                        .queryParam("key", apiKey)
                        .queryParam("site", "stackoverflow")
                        .queryParam("sort", "creation")
                        .queryParam("order", "desc")
                        .queryParam("min", min)
                        .queryParam("filter", "withbody")
                        .build(questionId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<StackOverflowResponse<StackOverflowComment>>() {})
                .map(StackOverflowResponse::items);
    }

    public Mono<List<StackOverflowComment>> getCommentsToAnswer(List<Long> answerIds, Long min) {
        if (answerIds.isEmpty()) {
            return Mono.just(List.of());
        }

        String idsParam =
                String.join(";", answerIds.stream().map(String::valueOf).toList());

        return stackOverflowClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/answers/{ids}/comments")
                        .queryParam("key", apiKey)
                        .queryParam("site", "stackoverflow")
                        .queryParam("sort", "creation")
                        .queryParam("order", "desc")
                        .queryParam("min", min)
                        .queryParam("filter", "withbody")
                        .build(idsParam))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<StackOverflowResponse<StackOverflowComment>>() {})
                .map(StackOverflowResponse::items);
    }
}
