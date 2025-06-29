package backend.academy.scrapper.service;

import backend.academy.scrapper.repository.TagOperationRepository;
import backend.academy.shared.dto.AddTagRequest;
import backend.academy.shared.dto.RemoveTagRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {
    private final TagOperationRepository tagOperationRepository;

    public void addTagToLink(Long chatId, AddTagRequest addTagRequest) {
        tagOperationRepository.addTagToLink(chatId, addTagRequest.url(), addTagRequest.tag());
        log.atInfo()
                .setMessage("Successfully added tag to link")
                .addKeyValue("chatId", chatId)
                .addKeyValue("url", addTagRequest.url())
                .addKeyValue("tag", addTagRequest.tag())
                .log();
    }

    public void removeTagFromLink(Long chatId, RemoveTagRequest removeTagRequest) {
        tagOperationRepository.removeTagFromLink(chatId, removeTagRequest.url(), removeTagRequest.tag());
        log.atInfo()
                .setMessage("Successfully removed tag from link")
                .addKeyValue("chatId", chatId)
                .addKeyValue("url", removeTagRequest.url())
                .addKeyValue("tag", removeTagRequest.tag())
                .log();
    }
}
