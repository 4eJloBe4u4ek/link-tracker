package backend.academy.scrapper.service;

import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.shared.dto.AddLinkRequest;
import backend.academy.shared.dto.LinkMapper;
import backend.academy.shared.dto.LinkResponse;
import backend.academy.shared.dto.ListLinksResponse;
import backend.academy.shared.dto.RemoveLinkRequest;
import backend.academy.shared.dto.TrackedLink;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkService {
    private final LinkOperationRepository linkOperationRepository;

    public ListLinksResponse getLinksByChat(Long chatId) {
        int page = 0;
        List<TrackedLink> trackedLinks;
        List<LinkResponse> linkResponseList = new ArrayList<>();
        do {
            trackedLinks = linkOperationRepository.getLinksByChat(chatId, page++);
            trackedLinks.forEach(trackedLink -> linkResponseList.add(LinkMapper.toLinkResponse(trackedLink)));
        } while (!trackedLinks.isEmpty());

        log.atInfo()
                .setMessage("Successfully retrieved links by chat")
                .addKeyValue("chatId", chatId)
                .log();
        return new ListLinksResponse(linkResponseList, linkResponseList.size());
    }

    public LinkResponse addLink(Long chatId, AddLinkRequest addLinkRequest) {
        LinkResponse linkResponse = LinkMapper.toLinkResponse(linkOperationRepository.addLink(
                chatId, addLinkRequest.link(), addLinkRequest.tags(), addLinkRequest.filters()));
        log.atInfo()
                .setMessage("Successfully added link")
                .addKeyValue("chatId", chatId)
                .addKeyValue("link", addLinkRequest.link())
                .log();
        return linkResponse;
    }

    public LinkResponse removeLink(Long chatId, RemoveLinkRequest removeLinkRequest) {
        LinkResponse linkResponse =
                LinkMapper.toLinkResponse(linkOperationRepository.removeLink(chatId, removeLinkRequest.link()));
        log.atInfo()
                .setMessage("Successfully removed link")
                .addKeyValue("chatId", chatId)
                .addKeyValue("link", removeLinkRequest.link())
                .log();
        return linkResponse;
    }

    public ListLinksResponse getLinksByChatAndTag(Long chatId, String tag) {
        int page = 0;
        List<TrackedLink> trackedLinks;
        List<LinkResponse> linkResponseList = new ArrayList<>();
        do {
            trackedLinks = linkOperationRepository.getLinksByChatAndTag(chatId, tag, page++);
            trackedLinks.forEach(trackedLink -> linkResponseList.add(LinkMapper.toLinkResponse(trackedLink)));
        } while (!trackedLinks.isEmpty());

        log.atInfo()
                .setMessage("Successfully retrieved links by chat and tag")
                .addKeyValue("chatId", chatId)
                .addKeyValue("tag", tag)
                .log();
        return new ListLinksResponse(linkResponseList, linkResponseList.size());
    }
}
