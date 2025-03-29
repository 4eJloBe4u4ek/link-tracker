package backend.academy.scrapper.repository.jdbc;

import backend.academy.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.repository.ChatOperationRepository;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "app.access-type", havingValue = "SQL")
public class SqlChatRepository extends BaseSqlRepository implements ChatOperationRepository {
    private static final String REGISTER_CHAT = "INSERT INTO chats (id, created_at) VALUES (?, ?)";
    private static final String DELETE_CHAT = "DELETE FROM chats WHERE id = ?";

    public SqlChatRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Transactional
    @Override
    public void registerChat(Long chatId) {
        if (exists(CHECK_CHAT_EXISTS, chatId)) {
            throw new ChatAlreadyExistsException("Чат уже существует");
        }

        jdbcTemplate.update(REGISTER_CHAT, chatId, LocalDateTime.now());
    }

    @Transactional
    @Override
    public void deleteChat(Long chatId) {
        if (!exists(CHECK_CHAT_EXISTS, chatId)) {
            throw new ChatNotFoundException("Чата не существует");
        }

        jdbcTemplate.update(DELETE_CHAT, chatId);
    }
}
