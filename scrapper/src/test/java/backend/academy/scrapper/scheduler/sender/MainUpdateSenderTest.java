package backend.academy.scrapper.scheduler.sender;

import static backend.academy.scrapper.TestData.LINK_UPDATE;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.config.ScrapperConfig;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class MainUpdateSenderTest {
    @Mock
    private ScrapperConfig config;

    @Mock
    private HttpUpdateSender httpUpdateSender;

    @Mock
    private KafkaUpdateSender kafkaUpdateSender;

    private MainUpdateSender mainUpdateSender;

    @Test
    void shouldUseHttpWhenConfiguredAsHttp() {
        // Arrange
        when(config.messageTransport()).thenReturn(ScrapperConfig.MessageTransport.HTTP);
        mainUpdateSender = new MainUpdateSender(config, httpUpdateSender, Optional.of(kafkaUpdateSender));
        when(httpUpdateSender.sendUpdate(LINK_UPDATE)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(mainUpdateSender.sendUpdate(LINK_UPDATE)).verifyComplete();
        verify(httpUpdateSender, times(1)).sendUpdate(LINK_UPDATE);
        verify(kafkaUpdateSender, never()).sendUpdate(ArgumentMatchers.any());
    }

    @Test
    void shouldUseKafkaWhenConfiguredAsKafka() {
        // Arrange
        when(config.messageTransport()).thenReturn(ScrapperConfig.MessageTransport.KAFKA);
        mainUpdateSender = new MainUpdateSender(config, httpUpdateSender, Optional.of(kafkaUpdateSender));
        when(kafkaUpdateSender.sendUpdate(LINK_UPDATE)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(mainUpdateSender.sendUpdate(LINK_UPDATE)).verifyComplete();
        verify(kafkaUpdateSender, times(1)).sendUpdate(LINK_UPDATE);
        verify(httpUpdateSender, never()).sendUpdate(ArgumentMatchers.any());
    }

    @Test
    void shouldFallbackToKafkaOnHttpError() {
        // Arrange
        when(config.messageTransport()).thenReturn(ScrapperConfig.MessageTransport.HTTP);
        mainUpdateSender = new MainUpdateSender(config, httpUpdateSender, Optional.of(kafkaUpdateSender));
        when(httpUpdateSender.sendUpdate(LINK_UPDATE))
                .thenReturn(Mono.error(new RuntimeException("Exception message")));
        when(kafkaUpdateSender.sendUpdate(LINK_UPDATE)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(mainUpdateSender.sendUpdate(LINK_UPDATE)).verifyComplete();
        verify(httpUpdateSender, times(1)).sendUpdate(LINK_UPDATE);
        verify(kafkaUpdateSender, times(1)).sendUpdate(LINK_UPDATE);
    }

    @Test
    void shouldFallbackToHttpOnKafkaError() {
        // Arrange
        when(config.messageTransport()).thenReturn(ScrapperConfig.MessageTransport.KAFKA);
        mainUpdateSender = new MainUpdateSender(config, httpUpdateSender, Optional.of(kafkaUpdateSender));
        when(kafkaUpdateSender.sendUpdate(LINK_UPDATE))
                .thenReturn(Mono.error(new RuntimeException("Exception message")));
        when(httpUpdateSender.sendUpdate(LINK_UPDATE)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(mainUpdateSender.sendUpdate(LINK_UPDATE)).verifyComplete();
        verify(kafkaUpdateSender, times(1)).sendUpdate(LINK_UPDATE);
        verify(httpUpdateSender, times(1)).sendUpdate(LINK_UPDATE);
    }

    @Test
    void shouldPropagateHttpErrorWhenKafkaIsDisabled() {
        // Arrange
        when(config.messageTransport()).thenReturn(ScrapperConfig.MessageTransport.HTTP);
        mainUpdateSender = new MainUpdateSender(config, httpUpdateSender, Optional.empty());
        when(httpUpdateSender.sendUpdate(LINK_UPDATE))
                .thenReturn(Mono.error(new RuntimeException("Exception message")));

        // Act & Assert
        StepVerifier.create(mainUpdateSender.sendUpdate(LINK_UPDATE))
                .expectErrorMessage("Exception message")
                .verify();
        verify(httpUpdateSender, times(1)).sendUpdate(LINK_UPDATE);
        verify(kafkaUpdateSender, never()).sendUpdate(ArgumentMatchers.any());
    }
}
