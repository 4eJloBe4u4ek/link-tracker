package backend.academy.scrapper.scheduler.sender;

import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.shared.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class KafkaUpdateSender implements UpdateSender {
    private final KafkaTemplate<Long, LinkUpdate> kafkaTemplate;
    private final ScrapperConfig scrapperConfig;

    @Override
    public Mono<Void> sendUpdate(LinkUpdate linkUpdate) {
        return Mono.fromFuture(
                        () -> kafkaTemplate.send(scrapperConfig.updateEvents().topic(), linkUpdate))
                .then()
                .doOnError(e -> log.atInfo()
                        .setMessage("Error sending update via Kafka")
                        .addKeyValue("linkUpdate", linkUpdate)
                        .addKeyValue("error", e)
                        .log());
    }
}
