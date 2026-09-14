package backend.academy.scrapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class ScrapperConfigTest {
    private static final String TICKETPRO_BASE_URL = "https://www.ticketpro.by";
    private static final String[] BASE_PROPERTIES = {"app.batch-size=100"};

    @Test
    void shouldCreateConfigWhenTicketproSettingsAreValid() {
        // Arrange
        ApplicationContextRunner runner = contextRunner("app.ticketpro.base-url=" + TICKETPRO_BASE_URL);

        // Act
        runner.run(context -> {
            // Assert
            assertThat(context).hasSingleBean(ScrapperConfig.class);
            ScrapperConfig config = context.getBean(ScrapperConfig.class);
            assertThat(config.ticketpro().baseUrl()).isEqualTo(TICKETPRO_BASE_URL);
        });
    }

    @Test
    void shouldRejectConfigWhenTicketproSectionIsMissing() {
        // Arrange
        ApplicationContextRunner runner = contextRunner();

        // Act
        runner.run(context -> {
            // Assert
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class);
        });
    }

    @Test
    void shouldRejectConfigWhenTicketproBaseUrlIsEmpty() {
        // Arrange
        ApplicationContextRunner runner = contextRunner("app.ticketpro.base-url=");

        // Act
        runner.run(context -> {
            // Assert
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class);
        });
    }

    private ApplicationContextRunner contextRunner(String... properties) {
        return new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class)
                .withPropertyValues(BASE_PROPERTIES)
                .withPropertyValues(properties);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ScrapperConfig.class)
    static class TestConfiguration {}
}
