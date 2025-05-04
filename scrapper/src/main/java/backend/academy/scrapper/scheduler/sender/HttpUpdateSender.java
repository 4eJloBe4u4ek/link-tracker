package backend.academy.scrapper.scheduler.sender;

import backend.academy.scrapper.botclient.BotClient;
import backend.academy.shared.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpUpdateSender implements UpdateSender {
    private final BotClient botClient;

    @Override
    public Mono<Void> sendUpdate(LinkUpdate linkUpdate) {
        return botClient
                .updateLink(linkUpdate)
                .doOnSuccess(v -> log.atInfo()
                        .setMessage("HTTP update sent successfully")
                        .addKeyValue("linkUpdateId", linkUpdate.id())
                        .log())
                .doOnError(e -> log.atInfo()
                        .setMessage("Error sending update via HTTP")
                        .addKeyValue("linkUpdate", linkUpdate)
                        .addKeyValue("error", e)
                        .log());
    }
}
