package backend.academy.scrapper.repository.jdbc;

import backend.academy.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.repository.ChatOperationRepository;
import backend.academy.shared.dto.NotificationMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "app.access-type", havingValue = "SQL")
public class SqlChatRepository extends BaseSqlRepository implements ChatOperationRepository {
    private static final String REGISTER_CHAT =
            "INSERT INTO chats (id, created_at, notification_mode, digest_time) VALUES (?, ?, ?, ?)";
    private static final String DELETE_CHAT = "DELETE FROM chats WHERE id = ?";
    private static final String GET_NOTIFICATION_MODE_BY_CHAT =
            "SELECT chats.notification_mode FROM chats WHERE id = ?";
    private static final String GET_CHAT_IDS_WITH_CURRENT_DIGEST =
            """
                    SELECT chats.id
                    FROM chats
                    WHERE notification_mode = 'DAILY_DIGEST' AND digest_time = date_trunc('minute', now())::time
            """;
    private static final String UPDATE_NOTIFICATION_MODE =
            "UPDATE chats SET notification_mode = ?, digest_time = ? WHERE id = ?";

    public SqlChatRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Transactional
    @Override
    public void registerChat(Long chatId, NotificationMode mode, LocalTime digestTime) {
        if (exists(CHECK_CHAT_EXISTS, chatId)) {
            throw new ChatAlreadyExistsException("Чат уже существует");
        }

        jdbcTemplate.update(
                REGISTER_CHAT, chatId, LocalDateTime.now(ZoneId.systemDefault()), mode.toString(), digestTime);
    }

    @Transactional
    @Override
    public void deleteChat(Long chatId) {
        if (!exists(CHECK_CHAT_EXISTS, chatId)) {
            throw new ChatNotFoundException("Чата не существует");
        }

        jdbcTemplate.update(DELETE_CHAT, chatId);
    }

    @Transactional
    @Override
    public NotificationMode getNotificationMode(Long chatId) {
        if (!exists(CHECK_CHAT_EXISTS, chatId)) {
            throw new ChatNotFoundException("Чата не существует");
        }
        return jdbcTemplate.queryForObject(GET_NOTIFICATION_MODE_BY_CHAT, NotificationMode.class, chatId);
    }

    @Transactional
    @Override
    public List<Long> getChatIdsWithDigestTimeMatchingNow() {
        return jdbcTemplate.queryForList(GET_CHAT_IDS_WITH_CURRENT_DIGEST, Long.class);
    }

    @Transactional
    @Override
    public void updateNotificationMode(Long chatId, NotificationMode mode, LocalTime digestTime) {
        if (!exists(CHECK_CHAT_EXISTS, chatId)) {
            throw new ChatNotFoundException("Чата не существует");
        }
        jdbcTemplate.update(UPDATE_NOTIFICATION_MODE, mode.toString(), digestTime, chatId);
    }
}
