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

    public Mono<StackoverflowQuestionUpdates> getStackoverflowUpdates(Long questionId, LocalDateTime lastUpdatedAt) {
        Instant instant = lastUpdatedAt.atZone(ZoneId.systemDefault()).toInstant();
        Long min = instant.getEpochSecond();

        Mono<StackoverflowQuestion> questionMono = getQuestion(questionId);
        Mono<List<StackoverflowAnswer>> answersMono = getAnswers(questionId, min);
        Mono<List<StackoverflowComment>> commentsToQuestionMono = getCommentsToQuestion(questionId, min);
        Mono<List<StackoverflowComment>> commentsToAnswerMono = answersMono.flatMap(answers -> {
            List<Long> answersIds =
                    answers.stream().map(StackoverflowAnswer::answerId).toList();

            return getCommentsToAnswer(answersIds, min);
        });

        return Mono.zip(questionMono, answersMono, commentsToQuestionMono, commentsToAnswerMono)
                .map(tuple ->
                        new StackoverflowQuestionUpdates(tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()));
    }

    public Mono<StackoverflowQuestion> getQuestion(Long questionId) {
        return stackOverflowClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/questions/{ids}")
                        .queryParam("key", apiKey)
                        .queryParam("site", "stackoverflow")
                        .build(questionId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<StackoverflowResponse<StackoverflowQuestion>>() {})
                .flatMap(response -> response.items().isEmpty()
                        ? Mono.empty()
                        : Mono.just(response.items().getFirst()));
    }

    public Mono<List<StackoverflowAnswer>> getAnswers(Long questionId, Long min) {
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
                .bodyToMono(new ParameterizedTypeReference<StackoverflowResponse<StackoverflowAnswer>>() {})
                .map(StackoverflowResponse::items);
    }

    public Mono<List<StackoverflowComment>> getCommentsToQuestion(Long questionId, Long min) {
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
                .bodyToMono(new ParameterizedTypeReference<StackoverflowResponse<StackoverflowComment>>() {})
                .map(StackoverflowResponse::items);
    }

    public Mono<List<StackoverflowComment>> getCommentsToAnswer(List<Long> answerIds, Long min) {
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
                .bodyToMono(new ParameterizedTypeReference<StackoverflowResponse<StackoverflowComment>>() {})
                .map(StackoverflowResponse::items);
    }
}
