package backend.academy.scrapper.scheduler.service;

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
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
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
        TrackedLink link = new TrackedLink(
                1L, "https://github.com/owner/repo", List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now());
        List<Long> chats = List.of(1L, 2L);

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(linkOperationRepository.getFiltersForChatAndLink(any(), eq(link))).thenReturn(List.of());
        when(chatOperationRepository.getNotificationMode(1L)).thenReturn(NotificationMode.IMMEDIATE);
        when(chatOperationRepository.getNotificationMode(2L)).thenReturn(NotificationMode.DAILY_DIGEST);
        when(updateSender.sendUpdate(any())).thenReturn(Mono.empty());
        when(notificationService.getSubscribedChats(link)).thenReturn(chats);

        notificationService.sendUpdate(link, "Update message", "author").block();

        verify(updateSender)
                .sendUpdate(argThat(update -> update.url().equals(link.url())
                        && update.description().equals("Update message")
                        && update.tgChatIds().contains(1L)
                        && !update.tgChatIds().contains(2L)));
    }

    @Test
    void shouldSkipUserWithFilter() {
        TrackedLink link = new TrackedLink(
                1L, "https://github.com/owner/repo", List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now());
        List<Long> chats = List.of(1L);
        when(linkOperationRepository.getFiltersForChatAndLink(1L, link)).thenReturn(List.of("user=author"));
        when(notificationService.getSubscribedChats(link)).thenReturn(chats);

        notificationService.sendUpdate(link, "message", "author").block();

        verify(updateSender, never()).sendUpdate(any());
    }

    @Test
    void shouldAddToDailyDigest() {
        TrackedLink link = new TrackedLink(
                1L, "https://github.com/owner/repo", List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now());
        List<Long> chats = List.of(2L);

        when(linkOperationRepository.getFiltersForChatAndLink(2L, link)).thenReturn(List.of());
        when(chatOperationRepository.getNotificationMode(2L)).thenReturn(NotificationMode.DAILY_DIGEST);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(notificationService.getSubscribedChats(link)).thenReturn(chats);

        notificationService.sendUpdate(link, "Digest message", "author").block();

        verify(listOperations)
                .rightPush(
                        eq("digest:2"),
                        argThat(update -> update.url().equals(link.url())
                                && update.description().equals("Digest message")));
    }

    @Test
    void shouldSendDailyDigestAndDeleteAfterSending() {
        LinkUpdate digestUpdate = new LinkUpdate(1L, "https://github.com/repo", "Digest message", List.of(1L));
        when(chatOperationRepository.getChatIdsWithDigestTimeMatchingNow()).thenReturn(List.of(1L));
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("digest:1", 0, -1)).thenReturn(List.of(digestUpdate));
        when(updateSender.sendUpdate(digestUpdate)).thenReturn(Mono.empty());

        notificationService.sendDailyDigest();

        verify(updateSender).sendUpdate(digestUpdate);
        verify(redisTemplate).delete("digest:1");
    }

    @Test
    void shouldNotifyOnlySubscribedChats() {
        TrackedLink link = new TrackedLink(
                1L, "https://github.com/owner/repo", List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now());
        List<Long> subscribedChats = List.of(1L, 3L);

        when(notificationService.getSubscribedChats(link)).thenReturn(subscribedChats);
        when(linkOperationRepository.getFiltersForChatAndLink(any(), eq(link))).thenReturn(List.of());
        when(chatOperationRepository.getNotificationMode(1L)).thenReturn(NotificationMode.IMMEDIATE);
        when(chatOperationRepository.getNotificationMode(3L)).thenReturn(NotificationMode.DAILY_DIGEST);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(updateSender.sendUpdate(any())).thenReturn(Mono.empty());

        notificationService.sendUpdate(link, "New update", "author").block();

        verify(updateSender)
                .sendUpdate(argThat(update -> update.tgChatIds().contains(1L)
                        && !update.tgChatIds().contains(2L)
                        && !update.tgChatIds().contains(3L)));
        verify(listOperations)
                .rightPush(
                        eq("digest:3"),
                        argThat(update -> update.url().equals(link.url())
                                && update.description().equals("New update")));
    }
}
