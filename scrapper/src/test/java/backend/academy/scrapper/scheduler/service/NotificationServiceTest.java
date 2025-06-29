package backend.academy.scrapper.scheduler.service;

import static backend.academy.scrapper.TestData.GITHUB_TRACKED_LINK;
import static backend.academy.scrapper.TestData.LINK_UPDATE;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_TRACKED_LINK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.repository.ChatOperationRepository;
import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.scrapper.scheduler.sender.UpdateSender;
import backend.academy.shared.dto.LinkUpdate;
import backend.academy.shared.dto.NotificationMode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    private static final String UPDATE_MESSAGE = "updateMessage";
    private static final String UPDATE_AUTHOR = "updateAuthor";
    private static final String REDIS_DIGEST_KEY = "digest:";

    @Mock
    private LinkOperationRepository linkOperationRepository;

    @Mock
    private ChatOperationRepository chatOperationRepository;

    @Mock
    private UpdateSender updateSender;

    @Mock
    private RedisTemplate<String, LinkUpdate> redisTemplate;

    @Mock
    private ListOperations<String, LinkUpdate> listOperations;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService =
                new NotificationService(linkOperationRepository, chatOperationRepository, updateSender, redisTemplate);
    }

    @Test
    void shouldSendUpdateToImmediateUsers() {
        List<Long> chats = List.of(1L, 2L);

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(linkOperationRepository.getFiltersForChatAndLink(any(), eq(GITHUB_TRACKED_LINK)))
                .thenReturn(List.of());
        when(chatOperationRepository.getNotificationMode(1L)).thenReturn(NotificationMode.IMMEDIATE);
        when(chatOperationRepository.getNotificationMode(2L)).thenReturn(NotificationMode.DAILY_DIGEST);
        when(updateSender.sendUpdate(any())).thenReturn(Mono.empty());
        when(notificationService.getSubscribedChats(GITHUB_TRACKED_LINK)).thenReturn(chats);

        notificationService
                .sendUpdate(GITHUB_TRACKED_LINK, UPDATE_MESSAGE, UPDATE_AUTHOR)
                .block();

        verify(updateSender)
                .sendUpdate(argThat(update -> update.url().equals(GITHUB_TRACKED_LINK.url())
                        && update.description().equals(UPDATE_MESSAGE)
                        && update.tgChatIds().contains(1L)
                        && !update.tgChatIds().contains(2L)));
    }

    @Test
    void shouldSkipUserWithFilter() {
        List<Long> chats = List.of(1L);
        when(linkOperationRepository.getFiltersForChatAndLink(1L, STACKOVERFLOW_TRACKED_LINK))
                .thenReturn(List.of("user=author"));
        when(notificationService.getSubscribedChats(STACKOVERFLOW_TRACKED_LINK)).thenReturn(chats);

        notificationService
                .sendUpdate(STACKOVERFLOW_TRACKED_LINK, UPDATE_MESSAGE, UPDATE_AUTHOR)
                .block();

        verify(updateSender, never()).sendUpdate(any());
    }

    @Test
    void shouldAddToDailyDigest() {
        List<Long> chats = List.of(2L);

        when(linkOperationRepository.getFiltersForChatAndLink(2L, GITHUB_TRACKED_LINK))
                .thenReturn(List.of());
        when(chatOperationRepository.getNotificationMode(2L)).thenReturn(NotificationMode.DAILY_DIGEST);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(notificationService.getSubscribedChats(GITHUB_TRACKED_LINK)).thenReturn(chats);

        notificationService
                .sendUpdate(GITHUB_TRACKED_LINK, UPDATE_MESSAGE, UPDATE_AUTHOR)
                .block();

        verify(listOperations)
                .rightPush(
                        eq(REDIS_DIGEST_KEY + "2"),
                        argThat(update -> update.url().equals(GITHUB_TRACKED_LINK.url())
                                && update.description().equals(UPDATE_MESSAGE)));
    }

    @Test
    void shouldSendDailyDigestAndDeleteAfterSending() {
        when(chatOperationRepository.getChatIdsWithDigestTimeMatchingNow()).thenReturn(List.of(1L));
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(REDIS_DIGEST_KEY + "1", 0, -1)).thenReturn(List.of(LINK_UPDATE));
        when(updateSender.sendUpdate(LINK_UPDATE)).thenReturn(Mono.empty());

        notificationService.sendDailyDigest();

        verify(updateSender).sendUpdate(LINK_UPDATE);
        verify(redisTemplate).delete(REDIS_DIGEST_KEY + "1");
    }

    @Test
    void shouldNotifyOnlySubscribedChats() {
        List<Long> subscribedChats = List.of(1L, 3L);

        when(notificationService.getSubscribedChats(STACKOVERFLOW_TRACKED_LINK)).thenReturn(subscribedChats);
        when(linkOperationRepository.getFiltersForChatAndLink(any(), eq(STACKOVERFLOW_TRACKED_LINK)))
                .thenReturn(List.of());
        when(chatOperationRepository.getNotificationMode(1L)).thenReturn(NotificationMode.IMMEDIATE);
        when(chatOperationRepository.getNotificationMode(3L)).thenReturn(NotificationMode.DAILY_DIGEST);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(updateSender.sendUpdate(any())).thenReturn(Mono.empty());

        notificationService
                .sendUpdate(STACKOVERFLOW_TRACKED_LINK, UPDATE_MESSAGE, UPDATE_AUTHOR)
                .block();

        verify(updateSender)
                .sendUpdate(argThat(update -> update.tgChatIds().contains(1L)
                        && !update.tgChatIds().contains(2L)
                        && !update.tgChatIds().contains(3L)));
        verify(listOperations)
                .rightPush(
                        eq(REDIS_DIGEST_KEY + "3"),
                        argThat(update -> update.url().equals(STACKOVERFLOW_TRACKED_LINK.url())
                                && update.description().equals(UPDATE_MESSAGE)));
    }
}
