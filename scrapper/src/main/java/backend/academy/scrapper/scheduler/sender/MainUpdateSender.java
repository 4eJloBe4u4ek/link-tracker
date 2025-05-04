package backend.academy.scrapper.scheduler.sender;

import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.shared.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class MainUpdateSender implements UpdateSender {
    private final ScrapperConfig config;
    private final HttpUpdateSender httpUpdateSender;
    private final KafkaUpdateSender kafkaUpdateSender;

    @Override
    public Mono<Void> sendUpdate(LinkUpdate linkUpdate) {
        UpdateSender primarySender = config.messageTransport().equals(ScrapperConfig.MessageTransport.HTTP)
                ? httpUpdateSender
                : kafkaUpdateSender;
        UpdateSender fallbackSender = config.messageTransport().equals(ScrapperConfig.MessageTransport.HTTP)
                ? kafkaUpdateSender
                : httpUpdateSender;

        return primarySender.sendUpdate(linkUpdate).onErrorResume(e -> {
            log.atWarn()
                    .setMessage("Primary sender failed, using fallback")
                    .addKeyValue("error", e.getMessage())
                    .log();
            return fallbackSender.sendUpdate(linkUpdate);
        });
    }
}
