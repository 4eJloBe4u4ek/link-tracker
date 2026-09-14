package backend.academy.scrapper.client.ticketpro;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketproCheckResult {
    SUCCESS("success"),
    RATE_LIMITED("rate_limited"),
    FORBIDDEN("forbidden"),
    ANTIBOT("antibot"),
    INVALID_CONTENT("invalid_content"),
    PARSE_ERROR("parse_error"),
    TIMEOUT("timeout"),
    NETWORK_ERROR("network_error"),
    STORAGE_ERROR("storage_error"),
    DELIVERY_ERROR("delivery_error");

    private final String metricValue;
}
