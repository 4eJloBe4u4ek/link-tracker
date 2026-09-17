package backend.academy.scrapper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monitoring", ignoreUnknownFields = false)
public record MonitoringProperties(
        boolean enabled, String scrapperPingUrl, String ticketproPingUrl, String puppetTheatrePingUrl) {}
