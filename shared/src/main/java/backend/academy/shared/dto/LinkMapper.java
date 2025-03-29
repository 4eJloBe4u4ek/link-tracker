package backend.academy.shared.dto;

import org.springframework.stereotype.Component;

@Component
public class LinkMapper {
    public static LinkResponse toLinkResponse(TrackedLink trackedLink) {
        return new LinkResponse(trackedLink.id(), trackedLink.url(), trackedLink.tags(), trackedLink.filters());
    }
}
