package backend.academy.scrapper.repository;

import backend.academy.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryRepository {
    @Getter
    private final Map<Long, List<TrackedLink>> trackedLinks = new HashMap<>();

    private final AtomicLong linkIdGenerator = new AtomicLong(1);

    public void registerChat(Long chatId) {
        if (trackedLinks.containsKey(chatId)) {
            throw new ChatAlreadyExistsException("Чат уже существует");
        }

        trackedLinks.put(chatId, new ArrayList<>());
    }

    public void deleteChat(Long chatId) {
        if (!trackedLinks.containsKey(chatId)) {
            throw new ChatNotFoundException("Чата не существует");
        }

        trackedLinks.remove(chatId);
    }

    public List<TrackedLink> getLinks(Long chatId) {
        if (!trackedLinks.containsKey(chatId)) {
            throw new ChatNotFoundException("Чата не существует");
        }

        return trackedLinks.getOrDefault(chatId, List.of());
    }

    public TrackedLink addLink(Long chatId, String link, List<String> tags, List<String> filters) {
        for (TrackedLink trackedLink : trackedLinks.get(chatId)) {
            if (trackedLink.url().equals(link)) {
                throw new LinkAlreadyExistsException("Ссылка уже добавлена");
            }
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        TrackedLink trackedLink = new TrackedLink(linkIdGenerator.getAndIncrement(), link, tags, filters, now, now);
        trackedLinks.computeIfAbsent(chatId, k -> new ArrayList<>()).add(trackedLink);
        return trackedLink;
    }

    public TrackedLink removeLink(Long chatId, String link) {
        for (TrackedLink trackedLink : trackedLinks.get(chatId)) {
            if (trackedLink.url().equals(link)) {
                trackedLinks.get(chatId).remove(trackedLink);
                return trackedLink;
            }
        }

        throw new LinkNotFoundException("Ссылка не найдена");
    }

    public void updateLastCheckedTime(TrackedLink trackedLink, LocalDateTime lastCheckedTime) {
        trackedLink.updatedAt(lastCheckedTime);
    }

    public List<Long> getChatsForTrackedLink(String link) {
        List<Long> chatIds = new ArrayList<>();
        for (Map.Entry<Long, List<TrackedLink>> entry : trackedLinks.entrySet()) {
            for (TrackedLink trackedLink : entry.getValue()) {
                if (trackedLink.url().equals(link)) {
                    chatIds.add(entry.getKey());
                }
            }
        }

        return chatIds;
    }
}
