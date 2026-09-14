package backend.academy.scrapper.client.ticketpro;

public record TicketproEvent(String title, String date, String time, String price, String ticketUrl) {

    public String snapshotKey() {
        return ticketUrl + "|" + date + "|" + time;
    }
}
