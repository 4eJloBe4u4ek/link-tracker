package backend.academy.bot.service;

import backend.academy.bot.scrapperclient.ScrapperClient;
import backend.academy.shared.dto.AddFilterRequest;
import backend.academy.shared.dto.AddLinkRequest;
import backend.academy.shared.dto.AddTagRequest;
import backend.academy.shared.dto.LinkResponse;
import backend.academy.shared.dto.ListLinksResponse;
import backend.academy.shared.dto.NotificationMode;
import backend.academy.shared.dto.RegisterChatRequest;
import backend.academy.shared.dto.RemoveFilterRequest;
import backend.academy.shared.dto.RemoveLinkRequest;
import backend.academy.shared.dto.RemoveTagRequest;
import backend.academy.shared.dto.UpdateNotificationModeRequest;
import backend.academy.shared.exception.ApiException;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommandService {
    private final ScrapperClient scrapperClient;
    private final RedisTemplate<String, ListLinksResponse> redisTemplate;
    private static final String TRACKED_LINKS_KEY_PREFIX = "trackedLinks:chat:";

    public Mono<Boolean> registerChat(Long chatId, NotificationMode mode, LocalTime digestTime) {
        RegisterChatRequest registerChatRequest = new RegisterChatRequest(mode, digestTime);
        return scrapperClient
                .registerChat(chatId, registerChatRequest)
                .thenReturn(true)
                .onErrorResume(ApiException.class, ex -> {
                    log.atWarn()
                            .setMessage("Failed to register chat")
                            .addKeyValue("chatId", chatId)
                            .addKeyValue("error", ex.getMessage())
                            .log();
                    return Mono.just(false);
                });
    }

    public Mono<List<String>> getTrackedLinks(Long chatId) {
        ListLinksResponse cachedLinks = redisTemplate.opsForValue().get(TRACKED_LINKS_KEY_PREFIX + chatId);
        if (cachedLinks != null) {
            return Mono.just(cachedLinks.links().stream().map(LinkResponse::url).toList());
        }

        return scrapperClient
                .getTrackedLinks(chatId)
                .flatMap(listLinksResponse -> {
                    redisTemplate.opsForValue().set(TRACKED_LINKS_KEY_PREFIX + chatId, listLinksResponse);
                    return Mono.just(listLinksResponse.links().stream()
                            .map(LinkResponse::url)
                            .toList());
                })
                .onErrorResume(ApiException.class, ex -> {
                    log.atWarn()
                            .setMessage("Failed to get links for chat")
                            .addKeyValue("chatId", chatId)
                            .addKeyValue("error", ex.getMessage())
                            .log();
                    return Mono.just(List.of());
                });
    }

    public Mono<Boolean> trackLink(Long chatId, String link, List<String> tags, List<String> filters) {
        AddLinkRequest addLinkRequest = new AddLinkRequest(link, tags, filters);
        redisTemplate.delete(TRACKED_LINKS_KEY_PREFIX + chatId);
        return scrapperClient
                .addTrackedLink(chatId, addLinkRequest)
                .thenReturn(true)
                .onErrorResume(ApiException.class, ex -> {
                    log.atWarn()
                            .setMessage("Failed to track link")
                            .addKeyValue("chatId", chatId)
                            .addKeyValue("url", link)
                            .addKeyValue("error", ex.getMessage())
                            .log();
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> untrackLink(Long chatId, String link) {
        RemoveLinkRequest removeLinkRequest = new RemoveLinkRequest(link);
        redisTemplate.delete(TRACKED_LINKS_KEY_PREFIX + chatId);
        return scrapperClient
                .deleteTrackedLink(chatId, removeLinkRequest)
                .thenReturn(true)
                .onErrorResume(ApiException.class, ex -> {
                    log.atWarn()
                            .setMessage("Failed to untrack link")
                            .addKeyValue("chatId", chatId)
                            .addKeyValue("url", link)
                            .addKeyValue("error", ex.getMessage())
                            .log();
                    return Mono.just(false);
                });
    }

    public Mono<List<String>> getTrackedLinksByTag(Long chatId, String tag) {
        return scrapperClient
                .getTrackedLinksByTag(chatId, tag)
                .map(listLinksResponse -> listLinksResponse.links().stream()
                        .map(LinkResponse::url)
                        .toList())
                .onErrorResume(ApiException.class, ex -> {
                    log.atWarn()
                            .setMessage("Failed to get links for chat and tag")
                            .addKeyValue("chatId", chatId)
                            .addKeyValue("tag", tag)
                            .addKeyValue("error", ex.getMessage())
                            .log();
                    return Mono.just(List.of());
                });
    }

    public Mono<Boolean> addTagToTrackedLink(Long chatId, String url, String tag) {
        AddTagRequest addTagRequest = new AddTagRequest(url, tag);
        return scrapperClient
                .addTagToTrackedLink(chatId, addTagRequest)
                .thenReturn(true)
                .onErrorResume(ApiException.class, ex -> {
                    log.atWarn()
                            .setMessage("Failed to add tag to tracked link")
                            .addKeyValue("chatId", chatId)
                            .addKeyValue("url", url)
                            .addKeyValue("tag", tag)
                            .addKeyValue("error", ex.getMessage())
                            .log();
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> removeTagFromTrackedLink(Long chatId, String url, String tag) {
        RemoveTagRequest removeTagRequest = new RemoveTagRequest(url, tag);
        return scrapperClient
                .removeTagFromTrackedLink(chatId, removeTagRequest)
                .thenReturn(true)
                .onErrorResume(ApiException.class, ex -> {
                    log.atWarn()
                            .setMessage("Failed to remove tag from tracked link")
                            .addKeyValue("chatId", chatId)
                            .addKeyValue("url", url)
                            .addKeyValue("tag", tag)
                            .addKeyValue("error", ex.getMessage())
                            .log();
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> addFilterToTrackedLink(Long chatId, String url, String filter) {
        AddFilterRequest addFilterRequest = new AddFilterRequest(url, filter);
        return scrapperClient
                .addFilterToTrackedLink(chatId, addFilterRequest)
                .thenReturn(true)
                .onErrorResume(ApiException.class, ex -> {
                    log.atWarn()
                            .setMessage("Failed to add filter to tracked link")
                            .addKeyValue("chatId", chatId)
                            .addKeyValue("url", url)
                            .addKeyValue("filter", filter)
                            .addKeyValue("error", ex.getMessage())
                            .log();
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> removeFilterFromTrackedLink(Long chatId, String url, String filter) {
        RemoveFilterRequest removeFilterRequest = new RemoveFilterRequest(url, filter);
        return scrapperClient
                .removeFilterFromTrackedLink(chatId, removeFilterRequest)
                .thenReturn(true)
                .onErrorResume(ApiException.class, ex -> {
                    log.atWarn()
                            .setMessage("Failed to remove filter from tracked link")
                            .addKeyValue("chatId", chatId)
                            .addKeyValue("url", url)
                            .addKeyValue("filter", filter)
                            .addKeyValue("error", ex.getMessage())
                            .log();
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> updateNotificationMode(Long chatId, NotificationMode mode, LocalTime digestTime) {
        UpdateNotificationModeRequest updateNotificationModeRequest =
                new UpdateNotificationModeRequest(mode, digestTime);
        return scrapperClient
                .updateNotificationMode(chatId, updateNotificationModeRequest)
                .thenReturn(true)
                .onErrorResume(ApiException.class, ex -> {
                    log.atWarn()
                            .setMessage("Failed to update notification mode for chat")
                            .addKeyValue("chatId", chatId)
                            .addKeyValue("mode", mode)
                            .addKeyValue("digest time", digestTime)
                            .addKeyValue("error", ex.getMessage())
                            .log();
                    return Mono.just(false);
                });
    }
}
