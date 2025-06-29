package backend.academy.scrapper.service;

import backend.academy.scrapper.repository.FilterOperationRepository;
import backend.academy.shared.dto.AddFilterRequest;
import backend.academy.shared.dto.RemoveFilterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilterService {
    private final FilterOperationRepository filterOperationRepository;

    public void addFilterToLink(Long chatId, AddFilterRequest addFilterRequest) {
        filterOperationRepository.addFilterToLink(chatId, addFilterRequest.url(), addFilterRequest.filter());
        log.atInfo()
                .setMessage("Successfully added filter to link")
                .addKeyValue("chatId", chatId)
                .addKeyValue("url", addFilterRequest.url())
                .addKeyValue("filter", addFilterRequest.filter())
                .log();
    }

    public void removeFilterFromLink(Long chatId, RemoveFilterRequest removeFilterRequest) {
        filterOperationRepository.removeFilterFromLink(chatId, removeFilterRequest.url(), removeFilterRequest.filter());
        log.atInfo()
                .setMessage("Successfully removed filter from link")
                .addKeyValue("chatId", chatId)
                .addKeyValue("url", removeFilterRequest.url())
                .addKeyValue("filter", removeFilterRequest.filter())
                .log();
    }
}
