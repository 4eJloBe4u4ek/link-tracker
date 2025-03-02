package backend.academy.scrapper.service;

import backend.academy.scrapper.repository.InMemoryRepository;
import backend.academy.shared.dto.AddLinkRequest;
import backend.academy.shared.dto.LinkResponse;
import backend.academy.shared.dto.ListLinksResponse;
import backend.academy.shared.dto.RemoveLinkRequest;
import backend.academy.shared.dto.TrackedLink;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LinkService {
    private final InMemoryRepository inMemoryRepository;

    public LinkService(InMemoryRepository inMemoryRepository) {
        this.inMemoryRepository = inMemoryRepository;
    }

    public ListLinksResponse getLinks(Long chatId) {
        List<LinkResponse> links = inMemoryRepository.getLinks(chatId).stream()
                .map(trackedLink -> new LinkResponse(
                        trackedLink.id(), trackedLink.url(), trackedLink.tags(), trackedLink.filters()))
                .toList();

        return new ListLinksResponse(links, links.size());
    }

    public LinkResponse addLink(Long chatId, AddLinkRequest addLinkRequest) {
        TrackedLink trackedLink = inMemoryRepository.addLink(
                chatId, addLinkRequest.link(), addLinkRequest.tags(), addLinkRequest.filters());
        return new LinkResponse(trackedLink.id(), trackedLink.url(), trackedLink.tags(), trackedLink.filters());
    }

    public LinkResponse removeLink(Long chatId, RemoveLinkRequest removeLinkRequest) {
        TrackedLink trackedLink = inMemoryRepository.removeLink(chatId, removeLinkRequest.link());
        return new LinkResponse(trackedLink.id(), trackedLink.url(), trackedLink.tags(), trackedLink.filters());
    }
}
