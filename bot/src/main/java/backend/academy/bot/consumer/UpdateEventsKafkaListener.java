package backend.academy.bot.consumer;

import backend.academy.bot.service.UpdateService;
import backend.academy.shared.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateEventsKafkaListener {
    private final UpdateService updateService;

    @KafkaListener(
            topics = "${app.update-events.topic}",
            groupId = "${app.update-events.consumer-group-id}",
            concurrency = "${app.update-events.concurrency}")
    public void listenUpdates(ConsumerRecord<Long, LinkUpdate> record, Acknowledgment acknowledgment) {
        updateService.processUpdate(record.value());
        acknowledgment.acknowledge();
    }
}
