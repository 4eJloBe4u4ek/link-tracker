package backend.academy.scrapper.client.puppettheatre;

import java.time.LocalDate;
import java.time.LocalTime;

public record PuppetTheatreSession(String title, LocalDate date, LocalTime time, String ticketUrl) {
    public String snapshotKey() {
        return ticketUrl + "|" + date + "|" + time + "|" + title;
    }
}
