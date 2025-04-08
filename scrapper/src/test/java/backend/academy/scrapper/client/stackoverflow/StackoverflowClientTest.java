package backend.academy.scrapper.client.stackoverflow;

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
public class StackoverflowClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private StackoverflowClient stackoverflowClient;

    @BeforeEach
    void setUp() {
        ScrapperConfig.StackOverflowCredentials stackOverflowCredentials =
                new ScrapperConfig.StackOverflowCredentials("testKey", "testToken", wireMock.baseUrl());
        ScrapperConfig config = new ScrapperConfig(null, null, 100, null, null, stackOverflowCredentials, null);

        stackoverflowClient = new StackoverflowClient(config);
    }

    @Test
    void shouldReturnQuestion() {
        wireMock.stubFor(
                get(urlPathEqualTo("/questions/123"))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("testToken"))
                        .withQueryParam("key", equalTo("testKey"))
                        .withQueryParam("site", equalTo("stackoverflow"))
                        .willReturn(
                                aResponse()
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(
                                                """
                    {
                        "items": [
                            {
                                "question_id": 123,
                                "owner": {
                                    "account_id": 123,
                                    "display_name": "Noname",
                                    "reputation": 123,
                                    "user_id": 123
                                },
                                "creation_date": 123,
                                "last_activity_date": 123,
                                "last_edit_date": 123,
                                "title": "Test question title",
                                "is_answered": true,
                                "answer_count": 123
                            }
                        ]
                    }
                    """)));

        StepVerifier.create(stackoverflowClient.getQuestion(123L))
                .expectNextMatches(question -> question.questionId().equals(123L)
                        && question.title().equals("Test question title")
                        && question.owner().displayName().equals("Noname")
                        && question.isAnswered())
                .verifyComplete();
    }

    @Test
    void shouldReturnAnswers() {
        wireMock.stubFor(
                get(urlPathEqualTo("/questions/123/answers"))
                        .withQueryParam("key", equalTo("testKey"))
                        .withQueryParam("site", equalTo("stackoverflow"))
                        .withQueryParam("sort", equalTo("creation"))
                        .withQueryParam("order", equalTo("desc"))
                        .withQueryParam("min", equalTo("123"))
                        .withQueryParam("filter", equalTo("withbody"))
                        .willReturn(
                                aResponse()
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(
                                                """
                                        {
                                            "items": [
                                                {
                                                    "answer_id": 1,
                                                    "question_id": 123,
                                                    "owner": {
                                                        "account_id": 123,
                                                        "display_name": "Noname",
                                                        "reputation": 123,
                                                        "user_id": 123
                                                    },
                                                    "body": "This is an answer",
                                                    "creation_date": 123,
                                                    "last_activity_date": 123,
                                                    "last_edit_date": 123
                                                }
                                            ]
                                        }
                                        """)));

        StepVerifier.create(stackoverflowClient.getAnswers(123L, 123L))
                .expectNextMatches(answers -> answers.size() == 1
                        && answers.getFirst().answerId() == 1
                        && answers.getFirst().body().equals("This is an answer"))
                .verifyComplete();
    }

    @Test
    void shouldReturnCommentsToQuestion() {
        wireMock.stubFor(
                get(urlPathEqualTo("/questions/123/comments"))
                        .withQueryParam("key", equalTo("testKey"))
                        .withQueryParam("site", equalTo("stackoverflow"))
                        .withQueryParam("sort", equalTo("creation"))
                        .withQueryParam("order", equalTo("desc"))
                        .withQueryParam("min", equalTo("123"))
                        .withQueryParam("filter", equalTo("withbody"))
                        .willReturn(
                                aResponse()
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(
                                                """
                                        {
                                            "items": [
                                                {
                                                    "comment_id": 1,
                                                    "owner": {
                                                        "account_id": 123,
                                                        "display_name": "Noname",
                                                        "reputation": 123,
                                                        "user_id": 123
                                                    },
                                                    "body": "This is a comment",
                                                    "creation_date": 123
                                                }
                                            ]
                                        }
                                        """)));

        StepVerifier.create(stackoverflowClient.getCommentsToQuestion(123L, 123L))
                .expectNextMatches(comments -> comments.size() == 1
                        && comments.getFirst().commentId() == 1
                        && comments.getFirst().body().equals("This is a comment"))
                .verifyComplete();
    }

    @Test
    void shouldReturnCommentsToAnswer() {
        wireMock.stubFor(
                get(urlPathEqualTo("/answers/123/comments"))
                        .withQueryParam("key", equalTo("testKey"))
                        .withQueryParam("site", equalTo("stackoverflow"))
                        .withQueryParam("sort", equalTo("creation"))
                        .withQueryParam("order", equalTo("desc"))
                        .withQueryParam("min", equalTo("123"))
                        .withQueryParam("filter", equalTo("withbody"))
                        .willReturn(
                                aResponse()
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(
                                                """
                                        {
                                            "items": [
                                                {
                                                    "comment_id": 1,
                                                    "owner": {
                                                        "account_id": 123,
                                                        "display_name": "Noname",
                                                        "reputation": 123,
                                                        "user_id": 123
                                                    },
                                                    "body": "This is a comment",
                                                    "creation_date": 123
                                                }
                                            ]
                                        }
                                        """)));

        StepVerifier.create(stackoverflowClient.getCommentsToAnswer(List.of(123L), 123L))
                .expectNextMatches(comments -> comments.size() == 1
                        && comments.getFirst().commentId() == 1
                        && comments.getFirst().body().equals("This is a comment"))
                .verifyComplete();
    }

    @Test
    void shouldHandleEmptyResponse() {
        wireMock.stubFor(get(urlPathEqualTo("/questions/123"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"items\":[]}")));

        StepVerifier.create(stackoverflowClient.getQuestion(123L)).verifyComplete();
    }
}
