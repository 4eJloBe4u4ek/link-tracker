package backend.academy.bot.consumer;

import static backend.academy.bot.TestData.INVALID_JSON;
import static backend.academy.bot.TestData.TEST_BAD_LINK_UPDATE;
import static backend.academy.bot.TestData.TEST_GOOD_LINK_UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.bot.BaseIntegrationTest;
import backend.academy.bot.exception.LinkUpdateException;
import backend.academy.bot.service.UpdateService;
import backend.academy.shared.dto.LinkUpdate;
import com.pengrad.telegrambot.TelegramBot;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@Import(UpdateEventsKafkaListenerTest.TestConfig.class)
class UpdateEventsKafkaListenerTest extends BaseIntegrationTest {
    @Autowired
    private KafkaTemplate<Long, LinkUpdate> producer;

    @Autowired
    private KafkaTemplate<byte[], byte[]> dlqProducer;

    @MockitoBean
    private TelegramBot bot;

    @MockitoBean
    private UpdateService updateService;

    @Value("${app.update-events.topic}")
    private String topic;

    @Value("${app.update-events.dlq-topic}")
    private String dlq;

    @Test
    void shouldProcessValidMessage() {
        // Act
        producer.send(topic, TEST_GOOD_LINK_UPDATE);

        // Assert
        verify(updateService, timeout(3_000)).processUpdate(eq(TEST_GOOD_LINK_UPDATE));
        verify(bot, never()).execute(any());
    }

    @Test
    void shouldSendToDlqWhenInvalidJsonReceived() {
        // Arrange
        try (Consumer<byte[], byte[]> dlqConsumer = createDlqConsumer()) {
            dlqConsumer.subscribe(List.of(dlq));
            dlqConsumer.poll(Duration.ofMillis(500));

            // Act
            dlqProducer.send(topic, INVALID_JSON.getBytes());

            // Assert
            await().pollInterval(Duration.ofMillis(500))
                    .atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> {
                        ConsumerRecords<byte[], byte[]> records = dlqConsumer.poll(Duration.ofMillis(500));
                        assertThat(records).isNotEmpty();

                        ConsumerRecord<byte[], byte[]> record =
                                records.iterator().next();
                        assertThat(new String(record.value())).isEqualTo(INVALID_JSON);
                    });
        }
        verify(bot, never()).execute(any());
    }

    @Test
    void shouldSendToDlqWhenServiceFails() {
        // Arrange
        Mockito.doThrow(new LinkUpdateException("empty chat list"))
                .when(updateService)
                .processUpdate(eq(TEST_BAD_LINK_UPDATE));

        try (Consumer<byte[], byte[]> dlqConsumer = createDlqConsumer()) {
            dlqConsumer.subscribe(List.of(dlq));
            dlqConsumer.poll(Duration.ofMillis(500));

            // Act
            producer.send(topic, TEST_BAD_LINK_UPDATE);

            // Assert
            verify(updateService, timeout(3_000)).processUpdate(eq(TEST_BAD_LINK_UPDATE));
            await().pollInterval(Duration.ofMillis(500))
                    .atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> {
                        ConsumerRecords<byte[], byte[]> records = dlqConsumer.poll(Duration.ofMillis(500));
                        assertThat(records).isNotEmpty();
                    });
        }
        verify(bot, never()).execute(any());
    }

    Consumer<byte[], byte[]> createDlqConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-dlq-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        return new KafkaConsumer<>(props);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean(name = "testProducerFactory")
        public ProducerFactory<Long, LinkUpdate> producerFactory(KafkaProperties props) {
            Map<String, Object> cfg = new HashMap<>(props.buildProducerProperties());
            cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
            cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
            return new DefaultKafkaProducerFactory<>(cfg);
        }

        @Bean(name = "testKafkaTemplate")
        public KafkaTemplate<Long, LinkUpdate> kafkaTemplate(ProducerFactory<Long, LinkUpdate> pf) {
            return new KafkaTemplate<>(pf);
        }
    }
}
