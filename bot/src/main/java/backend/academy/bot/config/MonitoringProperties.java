package backend.academy.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monitoring", ignoreUnknownFields = false)
public record MonitoringProperties(String botPingUrl) {}
