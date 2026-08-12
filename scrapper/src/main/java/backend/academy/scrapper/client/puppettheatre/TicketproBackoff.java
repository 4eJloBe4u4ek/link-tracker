package backend.academy.scrapper.client.puppettheatre;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class TicketproBackoff {
    private final Clock clock;
    private final AtomicReference<Instant> blockedUntil = new AtomicReference<>();

    public boolean requestAllowed() {
        Instant deadline = blockedUntil.get();
        return deadline == null || !clock.instant().isBefore(deadline);
    }

    public void activate(Duration duration) {
        blockedUntil.set(clock.instant().plus(duration));
    }

    public void clear() {
        blockedUntil.set(null);
    }
}
