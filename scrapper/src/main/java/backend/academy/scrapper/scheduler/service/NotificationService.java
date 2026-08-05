package backend.academy.scrapper.scheduler.service;

import backend.academy.scrapper.repository.ChatOperationRepository;
import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.scrapper.scheduler.sender.UpdateSender;
import backend.academy.shared.dto.LinkUpdate;
import backend.academy.shared.dto.NotificationMode;
import backend.academy.shared.dto.TrackedLink;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final LinkOperationRepository linkOperationRepository;
    private final ChatOperationRepository chatOperationRepository;
    private final UpdateSender updateSender;
    private final RedisTemplate<String, LinkUpdate> redisTemplate;
    private final AtomicLong linkUpdateIdGenerator = new AtomicLong(1);
    private static final String USER_FILTER_PREFIX = "user=";
    private static final String DIGEST_KEY_PREFIX = "digest:";

    public Mono<Void> sendUpdate(TrackedLink trackedLink, String message) {
        return sendUpdate(trackedLink, message, Optional.empty());
    }

    public Mono<Void> sendUpdate(TrackedLink trackedLink, String message, String author) {
        return sendUpdate(trackedLink, message, Optional.of(author));
    }

    private Mono<Void> sendUpdate(TrackedLink trackedLink, String message, Optional<String> author) {
        List<Long> chatsToNotifyImmediate = new ArrayList<>();
        for (Long chatId : getSubscribedChats(trackedLink)) {
            List<String> filters = linkOperationRepository.getFiltersForChatAndLink(chatId, trackedLink);
            boolean excludedByAuthorFilter = author
                    .map(value -> filters.contains(USER_FILTER_PREFIX + value))
                    .orElse(false);
            if (excludedByAuthorFilter) {
                continue;
            }

            if (chatOperationRepository.getNotificationMode(chatId) == NotificationMode.IMMEDIATE) {
                chatsToNotifyImmediate.add(chatId);
            } else if (chatOperationRepository.getNotificationMode(chatId) == NotificationMode.DAILY_DIGEST) {
                String key = DIGEST_KEY_PREFIX + chatId;
                redisTemplate
                        .opsForList()
                        .rightPush(
                                key,
                                new LinkUpdate(
                                        linkUpdateIdGenerator.getAndIncrement(),
                                        trackedLink.url(),
                                        message,
                                        List.of(chatId)));
            }
        }

        return chatsToNotifyImmediate.isEmpty()
                ? Mono.empty()
                : updateSender.sendUpdate(new LinkUpdate(
                        linkUpdateIdGenerator.getAndIncrement(), trackedLink.url(), message, chatsToNotifyImmediate));
    }

    public void sendDailyDigest() {
        chatOperationRepository.getChatIdsWithDigestTimeMatchingNow().forEach(chatId -> {
            String key = DIGEST_KEY_PREFIX + chatId;
            List<LinkUpdate> linkUpdates = redisTemplate.opsForList().range(key, 0, -1);
            if (linkUpdates != null && !linkUpdates.isEmpty()) {
                linkUpdates.forEach(update -> updateSender
                        .sendUpdate(update)
                        .doOnSuccess(v -> redisTemplate.delete(key))
                        .subscribe());
            }
        });
    }

    public List<Long> getSubscribedChats(TrackedLink trackedLink) {
        int page = 0;
        List<Long> chatIds = new ArrayList<>();
        List<Long> batch;
        do {
            batch = linkOperationRepository.getChatsForLink(trackedLink, page++);
            chatIds.addAll(batch);
        } while (!batch.isEmpty());

        return chatIds;
    }
}
