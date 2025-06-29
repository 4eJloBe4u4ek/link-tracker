package backend.academy.scrapper.repository.jdbc;

import static backend.academy.scrapper.TestData.TEST_CHAT_ID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.BaseIntegrationTest;
import backend.academy.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.shared.dto.NotificationMode;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {"app.access-type=sql"})
class SqlChatRepositoryTest extends BaseIntegrationTest {
    private static final String COUNT_CHAT_BY_ID = "SELECT COUNT(*) FROM chats WHERE id = ?";
    private static final LocalTime TEST_LOCAL_TIME = LocalTime.of(9, 30);

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
        // Act
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null);

        // Assert
        Integer count = jdbcTemplate.queryForObject(COUNT_CHAT_BY_ID, Integer.class, TEST_CHAT_ID);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfChatAlreadyExists() {
        // Arrange
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null);

        // Act & Assert
        assertThrows(
                ChatAlreadyExistsException.class,
                () -> sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null));
    }

    @Test
    @Transactional
    void shouldDeleteChat() {
        // Arrange
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null);

        // Act
        sqlChatRepository.deleteChat(TEST_CHAT_ID);

        // Assert
        Integer count = jdbcTemplate.queryForObject(COUNT_CHAT_BY_ID, Integer.class, TEST_CHAT_ID);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfChatNotFound() {
        // Act & Assert
        Long otherChatId = 9999L;
        assertThrows(ChatNotFoundException.class, () -> sqlChatRepository.deleteChat(otherChatId));
    }

    @Test
    @Transactional
    void shouldReturnNotificationMode() {
        // Arrange
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.DAILY_DIGEST, TEST_LOCAL_TIME);

        // Act
        NotificationMode notificationMode = sqlChatRepository.getNotificationMode(TEST_CHAT_ID);

        // Assert
        assertThat(notificationMode).isEqualTo(NotificationMode.DAILY_DIGEST);
    }

    @Test
    @Transactional
    void shouldThrowExceptionWhenGettingNotificationModeOfNonexistentChat() {
        // Act & Assert
        Long otherChatId = 9999L;
        assertThrows(ChatNotFoundException.class, () -> sqlChatRepository.getNotificationMode(otherChatId));
    }

    @Test
    @Transactional
    void shouldUpdateNotificationMode() {
        // Arrange
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null);

        // Act
        sqlChatRepository.updateNotificationMode(TEST_CHAT_ID, NotificationMode.DAILY_DIGEST, TEST_LOCAL_TIME);

        // Assert
        NotificationMode updatedMode = sqlChatRepository.getNotificationMode(TEST_CHAT_ID);
        assertThat(updatedMode).isEqualTo(NotificationMode.DAILY_DIGEST);
    }

    @Test
    @Transactional
    void shouldThrowExceptionWhenUpdatingNotificationModeOfNonexistentChat() {
        // Act & Assert
        Long otherChatId = 9999L;
        assertThrows(
                ChatNotFoundException.class,
                () -> sqlChatRepository.updateNotificationMode(otherChatId, NotificationMode.IMMEDIATE, null));
    }

    @Test
    @Transactional
    void shouldReturnChatIdsWithDigestTimeMatchingNow() {
        // Arrange
        LocalTime nowTruncated = LocalTime.now().withSecond(0).withNano(0);
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.DAILY_DIGEST, nowTruncated);

        // Act
        List<Long> chatIds = sqlChatRepository.getChatIdsWithDigestTimeMatchingNow();

        // Assert
        assertThat(chatIds.contains(TEST_CHAT_ID)).isTrue();
    }

    @Test
    @Transactional
    void shouldReturnEmptyListWhenNoChatWithDigestTimeMatchingNow() {
        // Arrange
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.DAILY_DIGEST, TEST_LOCAL_TIME);

        // Act
        List<Long> chatIds = sqlChatRepository.getChatIdsWithDigestTimeMatchingNow();

        // Assert
        assertThat(chatIds.isEmpty()).isTrue();
    }
}
