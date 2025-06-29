package backend.academy.bot.scrapperclient;

import static backend.academy.bot.TestData.CLIENT_ERROR_RESPONSE;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import backend.academy.shared.dto.NotificationMode;
import backend.academy.shared.dto.RegisterChatRequest;
import backend.academy.shared.exception.ApiException;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.time.Duration;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest(
        classes = {
            backend.academy.bot.scrapperclient.ScrapperClient.class,
            backend.academy.bot.config.resilience.ResilienceConfig.class
        })
@EnableAutoConfiguration(
        exclude = {
            org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
            org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
        })
class ScrapperClientTest {
    public static final String TG_CHAT_PATH = "/tg-chat/";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().port(8081))
            .build();

    @Autowired
    private ScrapperClient client;

    @Autowired
    private Retry scrapperRetry;

    @Autowired
    private CircuitBreaker scrapperCircuitBreaker;

    @Autowired
    private TimeLimiter scrapperTimeLimiter;

    @BeforeEach
    void resetCircuitBreaker() {
        scrapperCircuitBreaker.reset();
    }

    @Test
    void shouldRetryOnServerErrorsAndSucceed() {
        int maxAttempts = scrapperRetry.getRetryConfig().getMaxAttempts();

        wireMock.stubFor(post(urlPathEqualTo(TG_CHAT_PATH + TEST_CHAT_ID))
                .inScenario("retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR))
                .willSetStateTo("retry1"));

        for (int i = 1; i < maxAttempts - 1; i++) {
            wireMock.stubFor(post(urlPathEqualTo(TG_CHAT_PATH + TEST_CHAT_ID))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry" + i)
                    .willReturn(aResponse().withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR))
                    .willSetStateTo("retry" + (i + 1)));
        }

        wireMock.stubFor(post(urlPathEqualTo(TG_CHAT_PATH + TEST_CHAT_ID))
                .inScenario("retry")
                .whenScenarioStateIs("retry" + (maxAttempts - 1))
                .willReturn(aResponse().withStatus(HttpStatus.SC_OK)));

        Mono<Void> call = client.registerChat(TEST_CHAT_ID, new RegisterChatRequest(NotificationMode.IMMEDIATE, null));

        StepVerifier.create(call).expectComplete().verify();

        wireMock.verify(maxAttempts, postRequestedFor(urlPathEqualTo(TG_CHAT_PATH + TEST_CHAT_ID)));
    }

    @Test
    void shouldNotRetryOnClientError() {
        wireMock.stubFor(post(urlPathEqualTo(TG_CHAT_PATH + TEST_CHAT_ID))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_BAD_REQUEST)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(CLIENT_ERROR_RESPONSE)));

        Mono<Void> call = client.registerChat(TEST_CHAT_ID, new RegisterChatRequest(NotificationMode.IMMEDIATE, null));

        StepVerifier.create(call)
                .expectErrorSatisfies(err -> assertInstanceOf(ApiException.class, err))
                .verify(Duration.ofSeconds(2));

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(TG_CHAT_PATH + TEST_CHAT_ID)));
    }

    @Test
    void shouldTimeoutWhenResponseIsDelayed() {
        long timeoutMillis =
                scrapperTimeLimiter.getTimeLimiterConfig().getTimeoutDuration().toMillis();

        wireMock.stubFor(post(urlPathEqualTo(TG_CHAT_PATH + TEST_CHAT_ID))
                .willReturn(aResponse().withFixedDelay((int) timeoutMillis + 100)));

        Mono<Void> call = client.registerChat(TEST_CHAT_ID, new RegisterChatRequest(NotificationMode.IMMEDIATE, null));

        StepVerifier.create(call)
                .expectErrorMatches(e -> e instanceof java.util.concurrent.TimeoutException)
                .verify();

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(TG_CHAT_PATH + TEST_CHAT_ID)));
    }

    @Test
    void shouldOpenCircuitBreakerAfterConsecutiveFailures() {
        int minCalls = scrapperCircuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls();

        wireMock.stubFor(post(urlPathEqualTo(TG_CHAT_PATH + TEST_CHAT_ID))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_BAD_REQUEST)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(CLIENT_ERROR_RESPONSE)));

        for (int i = 0; i < minCalls; i++) {
            StepVerifier.create(client.registerChat(
                            TEST_CHAT_ID, new RegisterChatRequest(NotificationMode.IMMEDIATE, null)))
                    .expectError(ApiException.class)
                    .verify();
        }

        StepVerifier.create(
                        client.registerChat(TEST_CHAT_ID, new RegisterChatRequest(NotificationMode.IMMEDIATE, null)))
                .expectError(CallNotPermittedException.class)
                .verify();

        wireMock.verify(minCalls, postRequestedFor(urlPathEqualTo(TG_CHAT_PATH + TEST_CHAT_ID)));
    }
}
