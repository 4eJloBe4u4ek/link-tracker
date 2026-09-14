package backend.academy.scrapper.client.ticketpro;

import java.util.List;

public record TicketproVenue(String name, List<TicketproEvent> events) {
    public TicketproVenue {
        events = List.copyOf(events);
    }
}
