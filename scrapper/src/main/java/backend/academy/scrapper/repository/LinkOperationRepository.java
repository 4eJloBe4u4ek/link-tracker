package backend.academy.scrapper.repository;

import backend.academy.shared.dto.LinkType;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.util.List;

public interface LinkOperationRepository {
    List<TrackedLink> getLinksByChat(Long chatId, int page);

    List<TrackedLink> getAllLinks(int page);

    List<TrackedLink> getLinksByChatAndTag(Long chatId, String tag, int page);

    List<Long> getChatsForLink(TrackedLink trackedLink, int page);

    TrackedLink addLink(Long chatId, String link, List<String> tags, List<String> filters);

    TrackedLink removeLink(Long chatId, String link);

    void updateLastCheckedTime(TrackedLink trackedLink, LocalDateTime lastCheckedTime);

    List<String> getFiltersForChatAndLink(Long chatId, TrackedLink trackedLink);

    Long countByType(LinkType linkType);
}
