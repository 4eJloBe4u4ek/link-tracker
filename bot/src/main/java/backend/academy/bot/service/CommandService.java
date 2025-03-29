package backend.academy.bot.service;

import backend.academy.bot.scrapperclient.ScrapperClient;
import backend.academy.shared.dto.AddLinkRequest;
import backend.academy.shared.dto.AddTagRequest;
import backend.academy.shared.dto.LinkResponse;
import backend.academy.shared.dto.RemoveLinkRequest;
import backend.academy.shared.dto.RemoveTagRequest;
import backend.academy.shared.exception.ApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommandService {
    private final ScrapperClient scrapperClient;

    public Mono<Boolean> registerChat(Long chatId) {
        return scrapperClient.registerChat(chatId).thenReturn(true).onErrorResume(ApiException.class, ex -> {
            log.atWarn()
                    .setMessage("Failed to register chat")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("error", ex.getMessage())
                    .log();
            return Mono.just(false);
        });
    }

    public Mono<List<String>> getTrackedLinks(Long chatId) {
        return scrapperClient
                .getTrackedLinks(chatId)
                .map(listLinksResponse -> listLinksResponse.links().stream()
                        .map(LinkResponse::url)
                        .toList())
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
}
