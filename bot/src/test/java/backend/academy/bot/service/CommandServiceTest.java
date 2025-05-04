package backend.academy.bot.service;

import static backend.academy.bot.TestData.REDIS_TRACKED_LINKS_KEY_TEMPLATE;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static backend.academy.bot.TestData.TEST_URL;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.BaseIntegrationTest;
import backend.academy.bot.scrapperclient.ScrapperClient;
import backend.academy.shared.dto.LinkResponse;
import backend.academy.shared.dto.ListLinksResponse;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Testcontainers
class CommandServiceTest extends BaseIntegrationTest {
    private RedisTemplate<String, ListLinksResponse> redisTemplate;
    private ScrapperClient scrapperClient;
    private CommandService commandService;

    @BeforeEach
    void setUp() {
        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();

        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();

        scrapperClient = mock(ScrapperClient.class);
        commandService = new CommandService(scrapperClient, redisTemplate);
    }

    @Test
    void getTrackedLinks_whenNotCached_shouldFetchFromClientAndCache() {
        List<LinkResponse> links = List.of(new LinkResponse(1L, TEST_URL, List.of(), List.of()));
        ListLinksResponse response = new ListLinksResponse(links, links.size());

        when(scrapperClient.getTrackedLinks(TEST_CHAT_ID)).thenReturn(Mono.just(response));

        Mono<List<String>> result = commandService.getTrackedLinks(TEST_CHAT_ID);

        StepVerifier.create(result).expectNext(List.of(TEST_URL)).verifyComplete();

        Mono<List<String>> cachedResult = commandService.getTrackedLinks(TEST_CHAT_ID);

        StepVerifier.create(cachedResult).expectNext(List.of(TEST_URL)).verifyComplete();

        verify(scrapperClient, times(1)).getTrackedLinks(TEST_CHAT_ID);
    }

    @Test
    void trackLink_shouldInvalidateCache() {
        when(scrapperClient.addTrackedLink(eq(TEST_CHAT_ID), any())).thenReturn(Mono.empty());

        Mono<Boolean> result = commandService.trackLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        StepVerifier.create(result).expectNext(true).verifyComplete();

        Assertions.assertNull(
                redisTemplate.opsForValue().get(String.format(REDIS_TRACKED_LINKS_KEY_TEMPLATE, TEST_CHAT_ID)));
        verify(scrapperClient).addTrackedLink(eq(TEST_CHAT_ID), any());
    }

    @Test
    void untrackLink_shouldInvalidateCache() {
        when(scrapperClient.deleteTrackedLink(eq(TEST_CHAT_ID), any())).thenReturn(Mono.empty());

        Mono<Boolean> result = commandService.untrackLink(TEST_CHAT_ID, TEST_URL);

        StepVerifier.create(result).expectNext(true).verifyComplete();

        Assertions.assertNull(
                redisTemplate.opsForValue().get(String.format(REDIS_TRACKED_LINKS_KEY_TEMPLATE, TEST_CHAT_ID)));
        verify(scrapperClient).deleteTrackedLink(eq(TEST_CHAT_ID), any());
    }
}
