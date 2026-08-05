package backend.academy.scrapper.client.stackoverflow;

import static backend.academy.scrapper.TestData.STACKOVERFLOW_ANSWERS_PATH;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_ANSWER_ID;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_ANSWER_RESPONSE;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_ANSWER_RESPONSE_BODY;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_COMMENTS_TO_ANSWER_PATH;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_COMMENTS_TO_QUESTION_PATH;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_COMMENT_RESPONSE;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_COMMENT_RESPONSE_BODY;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_EMPTY_RESPONSE;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_MIN;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_QUESTION_ID;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_QUESTION_PATH;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_QUESTION_RESPONSE;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_QUESTION_RESPONSE_TITLE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import backend.academy.scrapper.config.ScrapperConfig;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.test.StepVerifier;

@ExtendWith(WireMockExtension.class)
public class StackOverflowClientTest {
    private static final String QUERY_PARAM_KEY = "testKey";
    private static final String QUERY_PARAM_SITE = "stackoverflow";
    private static final String QUERY_PARAM_SORT = "creation";
    private static final String QUERY_PARAM_ORDER = "desc";
    private static final String QUERY_PARAM_MIN = "123";
    private static final String QUERY_PARAM_FILTER = "withbody";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private StackOverflowClient stackoverflowClient;

    @BeforeEach
    void setUp() {
        ScrapperConfig.StackOverflowCredentials stackOverflowCredentials =
                new ScrapperConfig.StackOverflowCredentials("testKey", "testToken", wireMock.baseUrl());
        ScrapperConfig config = new ScrapperConfig(null, null, 100, null, null, stackOverflowCredentials, null, null);

        stackoverflowClient = new StackOverflowClient(config);
    }

    @Test
    void shouldReturnQuestion() {
        // Arrange
        wireMock.stubFor(get(urlPathEqualTo(STACKOVERFLOW_QUESTION_PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("testToken"))
                .withQueryParam("key", equalTo(QUERY_PARAM_KEY))
                .withQueryParam("site", equalTo(QUERY_PARAM_SITE))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(STACKOVERFLOW_QUESTION_RESPONSE)));

        // Act & Assert
        StepVerifier.create(stackoverflowClient.getQuestion(STACKOVERFLOW_QUESTION_ID))
                .expectNextMatches(question -> question.questionId().equals(STACKOVERFLOW_QUESTION_ID)
                        && question.title().equals(STACKOVERFLOW_QUESTION_RESPONSE_TITLE))
                .verifyComplete();
    }

    @Test
    void shouldReturnAnswers() {
        // Arrange
        wireMock.stubFor(get(urlPathEqualTo(STACKOVERFLOW_ANSWERS_PATH))
                .withQueryParam("key", equalTo(QUERY_PARAM_KEY))
                .withQueryParam("site", equalTo(QUERY_PARAM_SITE))
                .withQueryParam("sort", equalTo(QUERY_PARAM_SORT))
                .withQueryParam("order", equalTo(QUERY_PARAM_ORDER))
                .withQueryParam("min", equalTo(QUERY_PARAM_MIN))
                .withQueryParam("filter", equalTo(QUERY_PARAM_FILTER))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(STACKOVERFLOW_ANSWER_RESPONSE)));

        // Act & Assert
        StepVerifier.create(stackoverflowClient.getAnswers(STACKOVERFLOW_QUESTION_ID, STACKOVERFLOW_MIN))
                .expectNextMatches(answers ->
                        answers.size() == 1 && answers.getFirst().body().equals(STACKOVERFLOW_ANSWER_RESPONSE_BODY))
                .verifyComplete();
    }

    @Test
    void shouldReturnCommentsToQuestion() {
        // Arrange
        wireMock.stubFor(get(urlPathEqualTo(STACKOVERFLOW_COMMENTS_TO_QUESTION_PATH))
                .withQueryParam("key", equalTo(QUERY_PARAM_KEY))
                .withQueryParam("site", equalTo(QUERY_PARAM_SITE))
                .withQueryParam("sort", equalTo(QUERY_PARAM_SORT))
                .withQueryParam("order", equalTo(QUERY_PARAM_ORDER))
                .withQueryParam("min", equalTo(QUERY_PARAM_MIN))
                .withQueryParam("filter", equalTo(QUERY_PARAM_FILTER))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(STACKOVERFLOW_COMMENT_RESPONSE)));

        // Act & Assert
        StepVerifier.create(stackoverflowClient.getCommentsToQuestion(STACKOVERFLOW_QUESTION_ID, STACKOVERFLOW_MIN))
                .expectNextMatches(comments ->
                        comments.size() == 1 && comments.getFirst().body().equals(STACKOVERFLOW_COMMENT_RESPONSE_BODY))
                .verifyComplete();
    }

    @Test
    void shouldReturnCommentsToAnswer() {
        // Arrange
        wireMock.stubFor(get(urlPathEqualTo(STACKOVERFLOW_COMMENTS_TO_ANSWER_PATH))
                .withQueryParam("key", equalTo(QUERY_PARAM_KEY))
                .withQueryParam("site", equalTo(QUERY_PARAM_SITE))
                .withQueryParam("sort", equalTo(QUERY_PARAM_SORT))
                .withQueryParam("order", equalTo(QUERY_PARAM_ORDER))
                .withQueryParam("min", equalTo(QUERY_PARAM_MIN))
                .withQueryParam("filter", equalTo(QUERY_PARAM_FILTER))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(STACKOVERFLOW_COMMENT_RESPONSE)));

        // Act & Assert
        StepVerifier.create(
                        stackoverflowClient.getCommentsToAnswer(List.of(STACKOVERFLOW_ANSWER_ID), STACKOVERFLOW_MIN))
                .expectNextMatches(comments ->
                        comments.size() == 1 && comments.getFirst().body().equals(STACKOVERFLOW_COMMENT_RESPONSE_BODY))
                .verifyComplete();
    }

    @Test
    void shouldHandleEmptyResponse() {
        // Arrange
        wireMock.stubFor(get(urlPathEqualTo(STACKOVERFLOW_QUESTION_PATH))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(STACKOVERFLOW_EMPTY_RESPONSE)));

        // Act & Assert
        StepVerifier.create(stackoverflowClient.getQuestion(STACKOVERFLOW_QUESTION_ID))
                .verifyComplete();
    }
}
