package backend.academy.scrapper.repository.jdbc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.TestcontainersConfiguration;
import backend.academy.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.shared.dto.NotificationMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {"app.access-type=sql"})
@Import(TestcontainersConfiguration.class)
class SqlChatRepositoryTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private SqlChatRepository sqlChatRepository;

    @BeforeEach
    void setUp() {
        sqlChatRepository = new SqlChatRepository(jdbcTemplate);
    }

    @Test
    @Transactional
    void shouldSaveChat() {
        Long chatId = 12345L;
        sqlChatRepository.registerChat(chatId, NotificationMode.IMMEDIATE, null);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chats WHERE id = ?", Integer.class, chatId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfChatAlreadyExists() {
        Long chatId = 12345L;
        sqlChatRepository.registerChat(chatId, NotificationMode.IMMEDIATE, null);

        assertThrows(
                ChatAlreadyExistsException.class,
                () -> sqlChatRepository.registerChat(chatId, NotificationMode.IMMEDIATE, null));
    }

    @Test
    @Transactional
    void shouldDeleteChat() {
        Long chatId = 12345L;
        sqlChatRepository.registerChat(chatId, NotificationMode.IMMEDIATE, null);

        sqlChatRepository.deleteChat(chatId);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chats WHERE id = ?", Integer.class, chatId);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfChatNotFound() {
        Long chatId = 99999L;

        assertThrows(ChatNotFoundException.class, () -> sqlChatRepository.deleteChat(chatId));
    }
}
