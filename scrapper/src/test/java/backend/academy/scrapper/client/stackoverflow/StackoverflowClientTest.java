package backend.academy.scrapper.client.stackoverflow;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import backend.academy.scrapper.config.ScrapperConfig;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
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

        ScrapperConfig config = new ScrapperConfig(null, null, stackOverflowCredentials);

        stackoverflowClient = new StackoverflowClient(config);
    }

    @Test
    void shouldReturnQuestion() {
        wireMock.stubFor(
                get(urlPathEqualTo("/questions/123"))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("testToken"))
                        .withQueryParam("key", equalTo("testKey"))
                        .withQueryParam("site", equalTo("stackoverflow"))
                        .withQueryParam("sort", equalTo("activity"))
                        .withQueryParam("order", equalTo("desc"))
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
}
