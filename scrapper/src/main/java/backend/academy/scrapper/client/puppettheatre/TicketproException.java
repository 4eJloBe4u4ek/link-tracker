package backend.academy.scrapper.client.puppettheatre;

import lombok.Getter;

@Getter
public class TicketproException extends IllegalStateException {
    private final TicketproCheckResult result;
    private final boolean reportable;

    public TicketproException(TicketproCheckResult result, String message) {
        this(result, message, null, true);
    }

    public TicketproException(TicketproCheckResult result, String message, Throwable cause) {
        this(result, message, cause, true);
    }

    private TicketproException(
            TicketproCheckResult result, String message, Throwable cause, boolean reportable) {
        super(message, cause);
        this.result = result;
        this.reportable = reportable;
    }

    public static TicketproException duringBackoff(TicketproCheckResult result) {
        return new TicketproException(result, "Ticketpro request skipped during backoff", null, false);
    }
}
