package backend.academy.scrapper.client.puppettheatre;

import java.util.Locale;

public enum PuppetTheatreCheckResult {
    SUCCESS,
    HTTP_403,
    HTTP_429,
    ANTIBOT,
    EMPTY_PAGE,
    PARSE_ERROR,
    TIMEOUT,
    NETWORK_ERROR,
    HTTP_ERROR,
    STORAGE_ERROR,
    DELIVERY_ERROR,
    SKIPPED_BACKOFF;

    public String metricValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
