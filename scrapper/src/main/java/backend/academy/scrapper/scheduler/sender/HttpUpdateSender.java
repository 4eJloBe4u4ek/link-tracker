package backend.academy.scrapper.scheduler.sender;

import backend.academy.scrapper.botclient.BotClient;
import backend.academy.shared.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.message-transport", havingValue = "HTTP")
public class HttpUpdateSender implements UpdateSender {
    private final BotClient botClient;

    @Override
    public Mono<Void> sendUpdate(LinkUpdate linkUpdate) {
        return botClient
                .updateLink(linkUpdate)
                .doOnError(e -> log.atInfo()
                        .setMessage("Error sending update")
                        .addKeyValue("linkUpdate", linkUpdate)
                        .addKeyValue("error", e)
                        .log())
                .retry(3)
                .onErrorResume(e -> {
                    log.atInfo()
                            .setMessage("The update was never sent after the retries")
                            .addKeyValue("linkUpdate", linkUpdate)
                            .addKeyValue("error", e)
                            .log();
                    return Mono.empty();
                });
    }
}
