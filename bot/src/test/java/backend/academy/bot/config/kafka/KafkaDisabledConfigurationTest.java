package backend.academy.bot.config.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.bot.consumer.UpdateEventsKafkaListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class KafkaDisabledConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(
            KafkaConsumerConfig.class, KafkaDlqProducerConfig.class, UpdateEventsKafkaListener.class);

    @Test
    void shouldNotCreateKafkaBeansWhenKafkaIsDisabled() {
        // Arrange
        ApplicationContextRunner kafkaDisabledContextRunner =
                contextRunner.withPropertyValues("kafka.enabled=false");

        // Act
        kafkaDisabledContextRunner.run(context -> {
            // Assert
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(UpdateEventsKafkaListener.class);
            assertThat(context).doesNotHaveBean(KafkaConsumerConfig.class);
            assertThat(context).doesNotHaveBean(KafkaDlqProducerConfig.class);
        });
    }
}
