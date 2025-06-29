package backend.academy.scrapper.scheduler.sender;

import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.shared.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.message-transport", havingValue = "KAFKA")
public class KafkaUpdateSender implements UpdateSender {
    private final KafkaTemplate<Long, LinkUpdate> kafkaTemplate;
    private final ScrapperConfig scrapperConfig;

    @Override
    public Mono<Void> sendUpdate(LinkUpdate linkUpdate) {
        return Mono.fromFuture(
                        () -> kafkaTemplate.send(scrapperConfig.updateEvents().topic(), linkUpdate))
                .then();
    }
}
