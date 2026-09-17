package backend.academy.scrapper.client.puppettheatre;

import java.util.List;

public record PuppetTheatrePage(List<String> months, List<PuppetTheatreSession> sessions) {}
