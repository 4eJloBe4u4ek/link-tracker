package backend.academy.scrapper.client.puppettheatre;

public record PuppetTheatreSession(String title, String date, String time, String price, String ticketUrl) {

    public String snapshotKey() {
        return ticketUrl + "|" + date + "|" + time;
    }
}
