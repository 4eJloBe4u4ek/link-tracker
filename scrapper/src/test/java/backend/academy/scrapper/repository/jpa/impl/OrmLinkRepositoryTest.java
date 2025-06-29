package backend.academy.scrapper.repository.jpa.impl;

import static backend.academy.scrapper.TestData.TEST_CHAT_ID;
import static backend.academy.scrapper.TestData.TEST_FILTER;
import static backend.academy.scrapper.TestData.TEST_TAG;
import static backend.academy.scrapper.TestData.TEST_URL;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.BaseIntegrationTest;
import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.scrapper.domain.entity.ChatEntity;
import backend.academy.scrapper.domain.entity.LinkEntity;
import backend.academy.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.repository.jpa.repo.ChatJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.ChatLinkFilterJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.ChatLinkTagJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.FilterJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.LinkJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.TagJpaRepository;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {"app.access-type=orm"})
class OrmLinkRepositoryTest extends BaseIntegrationTest {
    @Autowired
    private ChatJpaRepository chatJpaRepository;

    @Autowired
    private LinkJpaRepository linkJpaRepository;

    @Autowired
    private TagJpaRepository tagJpaRepository;

    @Autowired
    private FilterJpaRepository filterJpaRepository;

    @Autowired
    private ScrapperConfig scrapperConfig;

    @Autowired
    private ChatLinkFilterJpaRepository chatLinkFilterJpaRepository;

    @Autowired
    private ChatLinkTagJpaRepository chatLinkTagJpaRepository;

    private OrmLinkRepository ormLinkRepository;

    @BeforeEach
    void setUp() {
        ormLinkRepository = new OrmLinkRepository(
                chatJpaRepository,
                linkJpaRepository,
                tagJpaRepository,
                filterJpaRepository,
                scrapperConfig,
                chatLinkFilterJpaRepository,
                chatLinkTagJpaRepository);

        ChatEntity chat = new ChatEntity();
        chat.id(TEST_CHAT_ID);
        chat.createdAt(LocalDateTime.now());
        chatJpaRepository.save(chat);
    }

    @Test
    @Transactional
    void shouldAddLink() {
        TrackedLink trackedLink =
                ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(TEST_TAG), List.of(TEST_FILTER));

        assertThat(linkJpaRepository.findByUrl(TEST_URL)).isPresent();
        assertThat(trackedLink.url()).isEqualTo(TEST_URL);
        assertThat(trackedLink.tags().contains(TEST_TAG)).isTrue();
        assertThat(trackedLink.filters().contains(TEST_FILTER)).isTrue();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkAlreadyExists() {
        ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        assertThrows(
                LinkAlreadyExistsException.class,
                () -> ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of()));
    }

    @Test
    @Transactional
    void shouldRemoveLink() {
        ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        ormLinkRepository.removeLink(TEST_CHAT_ID, TEST_URL);

        assertThat(linkJpaRepository.findByUrl(TEST_URL)).isEmpty();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkDoesNotExist() {
        assertThrows(LinkNotFoundException.class, () -> ormLinkRepository.removeLink(TEST_CHAT_ID, TEST_URL));
    }

    @Test
    @Transactional
    void shouldUpdateLastCheckedTime() {
        TrackedLink trackedLink = ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());
        LocalDateTime lastCheckedTime = LocalDateTime.now();

        ormLinkRepository.updateLastCheckedTime(trackedLink, lastCheckedTime);
        LinkEntity link = linkJpaRepository.findByUrl(TEST_URL).orElseThrow();

        assertThat(link.updatedAt()).isEqualTo(lastCheckedTime);
    }

    @Test
    @Transactional
    void shouldReturnLinksByChat() {
        ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        List<TrackedLink> links = ormLinkRepository.getLinksByChat(TEST_CHAT_ID, 0);

        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(TEST_URL);
    }

    @Test
    @Transactional
    void shouldReturnAllLinks() {
        ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        List<TrackedLink> links = ormLinkRepository.getAllLinks(0);

        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(TEST_URL);
    }

    @Test
    @Transactional
    void shouldReturnLinksByChatAndTag() {
        ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(TEST_TAG), List.of());

        List<TrackedLink> links = ormLinkRepository.getLinksByChatAndTag(TEST_CHAT_ID, TEST_TAG, 0);

        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(TEST_URL);
        assertThat(links.getFirst().tags()).isEqualTo(List.of(TEST_TAG));
    }

    @Test
    @Transactional
    void shouldReturnChatsForLink() {
        TrackedLink trackedLink = ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        List<Long> chats = ormLinkRepository.getChatsForLink(trackedLink, 0);

        assertThat(chats.size()).isEqualTo(1);
        assertThat(chats.getFirst()).isEqualTo(TEST_CHAT_ID);
    }

    @Test
    @Transactional
    void shouldReturnFiltersForChatAndLink() {
        TrackedLink trackedLink = ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of(TEST_FILTER));

        List<String> filters = ormLinkRepository.getFiltersForChatAndLink(TEST_CHAT_ID, trackedLink);

        assertThat(filters.size()).isEqualTo(1);
        assertThat(filters.contains(TEST_FILTER)).isTrue();
    }
}
