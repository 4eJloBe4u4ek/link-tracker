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
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null);

        Integer count = jdbcTemplate.queryForObject(COUNT_CHAT_BY_ID, Integer.class, TEST_CHAT_ID);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfChatAlreadyExists() {
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null);

        assertThrows(
                ChatAlreadyExistsException.class,
                () -> sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null));
    }

    @Test
    @Transactional
    void shouldDeleteChat() {
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null);

        sqlChatRepository.deleteChat(TEST_CHAT_ID);

        Integer count = jdbcTemplate.queryForObject(COUNT_CHAT_BY_ID, Integer.class, TEST_CHAT_ID);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfChatNotFound() {
        Long otherChatId = 9999L;
        assertThrows(ChatNotFoundException.class, () -> sqlChatRepository.deleteChat(otherChatId));
    }

    @Test
    @Transactional
    void shouldReturnNotificationMode() {
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.DAILY_DIGEST, TEST_LOCAL_TIME);

        NotificationMode notificationMode = sqlChatRepository.getNotificationMode(TEST_CHAT_ID);

        assertThat(notificationMode).isEqualTo(NotificationMode.DAILY_DIGEST);
    }

    @Test
    @Transactional
    void shouldThrowExceptionWhenGettingNotificationModeOfNonexistentChat() {
        Long otherChatId = 9999L;

        assertThrows(ChatNotFoundException.class, () -> sqlChatRepository.getNotificationMode(otherChatId));
    }

    @Test
    @Transactional
    void shouldUpdateNotificationMode() {
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null);

        sqlChatRepository.updateNotificationMode(TEST_CHAT_ID, NotificationMode.DAILY_DIGEST, TEST_LOCAL_TIME);

        NotificationMode updatedMode = sqlChatRepository.getNotificationMode(TEST_CHAT_ID);
        assertThat(updatedMode).isEqualTo(NotificationMode.DAILY_DIGEST);
    }

    @Test
    @Transactional
    void shouldThrowExceptionWhenUpdatingNotificationModeOfNonexistentChat() {
        Long otherChatId = 9999L;

        assertThrows(
                ChatNotFoundException.class,
                () -> sqlChatRepository.updateNotificationMode(otherChatId, NotificationMode.IMMEDIATE, null));
    }

    @Test
    @Transactional
    void shouldReturnChatIdsWithDigestTimeMatchingNow() {
        LocalTime nowTruncated = LocalTime.now().withSecond(0).withNano(0);
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.DAILY_DIGEST, nowTruncated);

        List<Long> chatIds = sqlChatRepository.getChatIdsWithDigestTimeMatchingNow();

        assertThat(chatIds.contains(TEST_CHAT_ID)).isTrue();
    }

    @Test
    @Transactional
    void shouldReturnEmptyListWhenNoChatWithDigestTimeMatchingNow() {
        sqlChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.DAILY_DIGEST, TEST_LOCAL_TIME);

        List<Long> chatIds = sqlChatRepository.getChatIdsWithDigestTimeMatchingNow();

        assertThat(chatIds.isEmpty()).isTrue();
    }
}
