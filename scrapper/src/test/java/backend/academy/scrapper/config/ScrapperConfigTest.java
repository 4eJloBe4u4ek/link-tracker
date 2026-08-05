package backend.academy.scrapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class ScrapperConfigTest {
    private static final String TICKETPRO_BASE_URL = "https://www.ticketpro.by";
    private static final String VENUE_PATH = "/koncertnye-ploshhadki/belorusskij-gosudarstvennyj-teatr-kukol/";
    private static final String[] BASE_PROPERTIES = {"app.batch-size=100"};

    @Test
    void shouldCreateConfigWhenPuppetTheatreSettingsAreValid() {
        // Act & Assert
        contextRunner(
                        "app.puppet-theatre.ticketpro-base-url=" + TICKETPRO_BASE_URL,
                        "app.puppet-theatre.venue-path=" + VENUE_PATH)
                .run(context -> {
                    assertThat(context).hasSingleBean(ScrapperConfig.class);
                    ScrapperConfig config = context.getBean(ScrapperConfig.class);
                    assertThat(config.puppetTheatre().ticketproBaseUrl()).isEqualTo(TICKETPRO_BASE_URL);
                    assertThat(config.puppetTheatre().venuePath()).isEqualTo(VENUE_PATH);
                });
    }

    @Test
    void shouldRejectConfigWhenPuppetTheatreSectionIsMissing() {
        // Act & Assert
        contextRunner().run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class);
        });
    }

    @Test
    void shouldRejectConfigWhenTicketproBaseUrlIsEmpty() {
        // Act & Assert
        contextRunner("app.puppet-theatre.ticketpro-base-url=", "app.puppet-theatre.venue-path=" + VENUE_PATH)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class);
                });
    }

    @Test
    void shouldRejectConfigWhenVenuePathIsEmpty() {
        // Act & Assert
        contextRunner("app.puppet-theatre.ticketpro-base-url=" + TICKETPRO_BASE_URL, "app.puppet-theatre.venue-path=")
                .run(context -> {
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
