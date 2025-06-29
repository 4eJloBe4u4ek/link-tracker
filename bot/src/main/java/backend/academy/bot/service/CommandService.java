package backend.academy.bot.service;

import backend.academy.bot.scrapperclient.ScrapperClient;
import backend.academy.shared.dto.AddLinkRequest;
import backend.academy.shared.dto.LinkResponse;
import backend.academy.shared.dto.RemoveLinkRequest;
import backend.academy.shared.exception.ApiException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class CommandService {
    private final ScrapperClient scrapperClient;

    public CommandService(ScrapperClient scrapperClient) {
        this.scrapperClient = scrapperClient;
    }

    public Mono<Boolean> registerChat(Long chatId) {
        return scrapperClient
                .registerChat(chatId)
                .thenReturn(true)
                .onErrorResume(ApiException.class, ex -> Mono.just(false));
    }

    public Mono<List<String>> getTrackedLinks(Long chatId) {
        return scrapperClient
                .getTrackedLinks(chatId)
                .map(listLinksResponse -> listLinksResponse.links().stream()
                        .map(LinkResponse::url)
                        .toList())
                .onErrorResume(ApiException.class, ex -> Mono.just(List.of()));
    }

    public Mono<Boolean> trackLink(Long chatId, String link, List<String> tags, List<String> filters) {
        AddLinkRequest addLinkRequest = new AddLinkRequest(link, tags, filters);
        return scrapperClient
                .addTrackedLink(chatId, addLinkRequest)
                .thenReturn(true)
                .onErrorResume(ApiException.class, ex -> Mono.just(false));
    }

    public Mono<Boolean> untrackLink(Long chatId, String link) {
        RemoveLinkRequest removeLinkRequest = new RemoveLinkRequest(link);
        return scrapperClient
                .deleteTrackedLink(chatId, removeLinkRequest)
                .thenReturn(true)
                .onErrorResume(ApiException.class, ex -> Mono.just(false));
    }
}
