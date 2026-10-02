package backend.academy.scrapper.client.puppettheatre;

import java.time.Duration;

public class PuppetTheatreException extends IllegalStateException {
    private final PuppetTheatreCheckResult result;
    private final Integer statusCode;
    private final Duration retryAfter;

    public PuppetTheatreException(PuppetTheatreCheckResult result, String message) {
        this(result, message, null, null, Duration.ZERO);
    }

    public PuppetTheatreException(PuppetTheatreCheckResult result, String message, Throwable cause) {
        this(result, message, cause, null, Duration.ZERO);
    }

    public PuppetTheatreException(
            PuppetTheatreCheckResult result, String message, int statusCode, Duration retryAfter) {
        this(result, message, null, statusCode, retryAfter);
    }

    private PuppetTheatreException(
            PuppetTheatreCheckResult result, String message, Throwable cause, Integer statusCode, Duration retryAfter) {
        super(message, cause);
        this.result = result;
        this.statusCode = statusCode;
        this.retryAfter = retryAfter == null ? Duration.ZERO : retryAfter;
    }

    public PuppetTheatreCheckResult result() {
        return result;
    }

    public Duration retryAfter() {
        return retryAfter;
    }

    public Integer statusCode() {
        return statusCode;
    }
}
