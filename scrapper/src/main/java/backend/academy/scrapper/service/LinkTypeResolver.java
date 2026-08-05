package backend.academy.scrapper.service;

import backend.academy.scrapper.scheduler.handler.GithubUpdateHandler;
import backend.academy.scrapper.scheduler.handler.PuppetTheatreUpdateHandler;
import backend.academy.scrapper.scheduler.handler.StackOverflowUpdateHandler;
import backend.academy.shared.dto.LinkType;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LinkTypeResolver {
    public Optional<LinkType> resolve(String url) {
        if (GithubUpdateHandler.isGithubLink(url)) {
            return Optional.of(LinkType.GITHUB);
        }
        if (StackOverflowUpdateHandler.isStackoverflowLink(url)) {
            return Optional.of(LinkType.STACKOVERFLOW);
        }
        if (PuppetTheatreUpdateHandler.isPuppetTheatreLink(url)) {
            return Optional.of(LinkType.PUPPET_THEATRE);
        }
        return Optional.empty();
    }
}
