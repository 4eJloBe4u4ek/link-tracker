package backend.academy.scrapper.scheduler.sender;

import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.shared.dto.LinkUpdate;
import java.util.Optional;
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
    private final Optional<KafkaUpdateSender> kafkaUpdateSender;

    @Override
    public Mono<Void> sendUpdate(LinkUpdate linkUpdate) {
        return switch (config.messageTransport()) {
            case HTTP -> sendWithFallback(linkUpdate, httpUpdateSender, kafkaUpdateSender);
            case KAFKA -> kafkaUpdateSender
                    .<Mono<Void>>map(sender -> sendWithFallback(linkUpdate, sender, Optional.of(httpUpdateSender)))
                    .orElseGet(() -> Mono.error(new IllegalStateException("Kafka transport is disabled")));
        };
    }

    private Mono<Void> sendWithFallback(
            LinkUpdate linkUpdate, UpdateSender primarySender, Optional<? extends UpdateSender> fallbackSender) {
        return primarySender.sendUpdate(linkUpdate).onErrorResume(error -> {
            if (fallbackSender.isEmpty()) {
                return Mono.error(error);
            }
            log.atWarn()
                    .setMessage("Primary sender failed, using fallback")
                    .addKeyValue("error", error.getMessage())
                    .log();
            return fallbackSender.orElseThrow().sendUpdate(linkUpdate);
        });
    }
}
