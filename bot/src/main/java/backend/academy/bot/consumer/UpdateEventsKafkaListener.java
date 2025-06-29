package backend.academy.bot.consumer;

import backend.academy.bot.config.BotConfig;
import backend.academy.bot.service.UpdateService;
import backend.academy.shared.dto.LinkUpdate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateEventsKafkaListener {
    private final BotConfig botConfig;
    private final UpdateService updateService;
    private final KafkaTemplate<byte[], byte[]> dlqKafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.update-events.topic}",
            groupId = "${app.update-events.consumer-group-id}",
            concurrency = "${app.update-events.concurrency}")
    public void listenUpdates(ConsumerRecord<Long, LinkUpdate> record, Acknowledgment acknowledgment)
            throws JsonProcessingException {
        try {
            updateService.processUpdate(record.value());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            dlqKafkaTemplate.send(botConfig.updateEvents().dlqTopic(), objectMapper.writeValueAsBytes(record.value()));
            acknowledgment.acknowledge();
        }
    }
}
