package backend.academy.scrapper.config.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.scrapper.scheduler.sender.KafkaUpdateSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class KafkaDisabledConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(KafkaConfig.class, KafkaProducerConfig.class, KafkaUpdateSender.class);

    @Test
    void shouldNotCreateKafkaBeansWhenKafkaIsDisabled() {
        // Arrange
        ApplicationContextRunner kafkaDisabledContextRunner =
                contextRunner.withPropertyValues("kafka.enabled=false");

        // Act
        kafkaDisabledContextRunner.run(context -> {
            // Assert
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(KafkaUpdateSender.class);
            assertThat(context).doesNotHaveBean(KafkaConfig.class);
            assertThat(context).doesNotHaveBean(KafkaProducerConfig.class);
        });
    }
}
