package backend.academy.scrapper.repository.jpa.impl;

import static backend.academy.scrapper.TestData.TEST_CHAT_ID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.BaseIntegrationTest;
import backend.academy.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.repository.jpa.repo.ChatJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.FilterJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.LinkJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.TagJpaRepository;
import backend.academy.shared.dto.NotificationMode;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {"app.access-type=orm"})
class OrmChatRepositoryTest extends BaseIntegrationTest {
    private static final LocalTime TEST_LOCAL_TIME = LocalTime.of(9, 30);

    @Autowired
    private ChatJpaRepository chatJpaRepository;

    @Autowired
    private LinkJpaRepository linkJpaRepository;

    @Autowired
    private TagJpaRepository tagJpaRepository;

    @Autowired
    private FilterJpaRepository filterJpaRepository;

    private OrmChatRepository ormChatRepository;

    @BeforeEach
    void setUp() {
        ormChatRepository =
                new OrmChatRepository(chatJpaRepository, linkJpaRepository, tagJpaRepository, filterJpaRepository);
        ormChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null);
    }

    @Test
    @Transactional
    void shouldSaveChat() {
        assertThat(chatJpaRepository.findById(TEST_CHAT_ID)).isPresent();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfChatAlreadyExists() {
        assertThrows(
                ChatAlreadyExistsException.class,
                () -> ormChatRepository.registerChat(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null));
    }

    @Test
    @Transactional
    void shouldDeleteChat() {
        ormChatRepository.deleteChat(TEST_CHAT_ID);

        assertThat(chatJpaRepository.findById(TEST_CHAT_ID)).isNotPresent();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfChatNotFound() {
        Long otherChatId = 9999L;
        assertThrows(ChatNotFoundException.class, () -> ormChatRepository.deleteChat(otherChatId));
    }

    @Test
    @Transactional
    void shouldReturnNotificationMode() {
        NotificationMode notificationMode = ormChatRepository.getNotificationMode(TEST_CHAT_ID);

        assertThat(notificationMode).isEqualTo(NotificationMode.IMMEDIATE);
    }

    @Test
    @Transactional
    void shouldThrowExceptionWhenGettingNotificationModeOfNonexistentChat() {
        Long otherChatId = 9999L;
        assertThrows(ChatNotFoundException.class, () -> ormChatRepository.getNotificationMode(otherChatId));
    }

    @Test
    @Transactional
    void shouldUpdateNotificationMode() {
        ormChatRepository.updateNotificationMode(TEST_CHAT_ID, NotificationMode.DAILY_DIGEST, TEST_LOCAL_TIME);

        NotificationMode updatedMode = ormChatRepository.getNotificationMode(TEST_CHAT_ID);
        assertThat(updatedMode).isEqualTo(NotificationMode.DAILY_DIGEST);
    }

    @Test
    @Transactional
    void shouldThrowExceptionWhenUpdatingNotificationModeOfNonexistentChat() {
        Long otherChatId = 9999L;
        assertThrows(
                ChatNotFoundException.class,
                () -> ormChatRepository.updateNotificationMode(otherChatId, NotificationMode.IMMEDIATE, null));
    }

    @Test
    @Transactional
    void shouldReturnChatIdsWithDigestTimeMatchingNow() {
        LocalTime nowTruncated = LocalTime.now().withSecond(0).withNano(0);
        ormChatRepository.updateNotificationMode(TEST_CHAT_ID, NotificationMode.DAILY_DIGEST, nowTruncated);

        List<Long> chatIds = ormChatRepository.getChatIdsWithDigestTimeMatchingNow();

        assertThat(chatIds.contains(TEST_CHAT_ID)).isTrue();
    }

    @Test
    @Transactional
    void shouldReturnEmptyListWhenNoChatWithDigestTimeMatchingNow() {
        ormChatRepository.updateNotificationMode(TEST_CHAT_ID, NotificationMode.DAILY_DIGEST, TEST_LOCAL_TIME);

        List<Long> chatIds = ormChatRepository.getChatIdsWithDigestTimeMatchingNow();

        assertThat(chatIds.isEmpty()).isTrue();
    }
}
