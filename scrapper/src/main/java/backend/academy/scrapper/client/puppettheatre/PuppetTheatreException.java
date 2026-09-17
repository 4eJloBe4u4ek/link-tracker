package backend.academy.scrapper.client.puppettheatre;

import java.time.Duration;

public class PuppetTheatreException extends IllegalStateException {
    private final PuppetTheatreCheckResult result;
    private final Duration retryAfter;

    public PuppetTheatreException(PuppetTheatreCheckResult result, String message) {
        this(result, message, null, Duration.ZERO);
    }

    public PuppetTheatreException(PuppetTheatreCheckResult result, String message, Throwable cause) {
        this(result, message, cause, Duration.ZERO);
    }

    public PuppetTheatreException(PuppetTheatreCheckResult result, String message, Duration retryAfter) {
        this(result, message, null, retryAfter);
    }

    private PuppetTheatreException(
            PuppetTheatreCheckResult result, String message, Throwable cause, Duration retryAfter) {
        super(message, cause);
        this.result = result;
        this.retryAfter = retryAfter;
    }

    public PuppetTheatreCheckResult result() {
        return result;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
