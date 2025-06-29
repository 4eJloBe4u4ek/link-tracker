package backend.academy.scrapper.repository;

import backend.academy.shared.dto.NotificationMode;
import java.time.LocalTime;
import java.util.List;

public interface ChatOperationRepository {
    void registerChat(Long chatId, NotificationMode mode, LocalTime digestTime);

    void deleteChat(Long chatId);

    NotificationMode getNotificationMode(Long chatId);

    List<Long> getChatIdsWithDigestTimeMatchingNow();

    void updateNotificationMode(Long chatId, NotificationMode mode, LocalTime digestTime);
}
