package backend.academy.bot.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.scrapperclient.ScrapperClient;
import backend.academy.shared.dto.LinkResponse;
import backend.academy.shared.dto.ListLinksResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Testcontainers
class CommandServiceTest {

    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private RedisTemplate<String, ListLinksResponse> redisTemplate;
    private ScrapperClient scrapperClient;
    private CommandService commandService;

    @BeforeEach
    void setUp() {
        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory(redisContainer.getHost(), redisContainer.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();

        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();

        scrapperClient = mock(ScrapperClient.class);
        commandService = new CommandService(scrapperClient, redisTemplate);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void getTrackedLinks_whenNotCached_shouldFetchFromClientAndCache() {
        Long chatId = 123L;
        List<LinkResponse> links = List.of(new LinkResponse(1L, "url", List.of(), List.of()));
        ListLinksResponse response = new ListLinksResponse(links, links.size());

        when(scrapperClient.getTrackedLinks(chatId)).thenReturn(Mono.just(response));

        Mono<List<String>> result = commandService.getTrackedLinks(chatId);

        StepVerifier.create(result).expectNext(List.of("url")).verifyComplete();

        Mono<List<String>> cachedResult = commandService.getTrackedLinks(chatId);

        StepVerifier.create(cachedResult).expectNext(List.of("url")).verifyComplete();

        verify(scrapperClient, times(1)).getTrackedLinks(chatId);
    }

    @Test
    void trackLink_shouldInvalidateCache() {
        Long chatId = 123L;
        String url = "url";
        List<String> tags = List.of("tag");
        List<String> filters = List.of("filter");

        when(scrapperClient.addTrackedLink(eq(chatId), any())).thenReturn(Mono.empty());

        Mono<Boolean> result = commandService.trackLink(chatId, url, tags, filters);

        StepVerifier.create(result).expectNext(true).verifyComplete();

        Assertions.assertNull(redisTemplate.opsForValue().get("trackedLinks:chat:" + chatId));
        verify(scrapperClient).addTrackedLink(eq(chatId), any());
    }

    @Test
    void untrackLink_shouldInvalidateCache() {
        Long chatId = 123L;
        String url = "url";

        when(scrapperClient.deleteTrackedLink(eq(chatId), any())).thenReturn(Mono.empty());

        Mono<Boolean> result = commandService.untrackLink(chatId, url);

        StepVerifier.create(result).expectNext(true).verifyComplete();

        Assertions.assertNull(redisTemplate.opsForValue().get("trackedLinks:chat:" + chatId));
        verify(scrapperClient).deleteTrackedLink(eq(chatId), any());
    }
}
