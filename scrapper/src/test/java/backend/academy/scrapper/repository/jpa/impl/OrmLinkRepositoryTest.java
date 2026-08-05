package backend.academy.scrapper.repository.jpa.impl;

import static backend.academy.scrapper.TestData.PUPPET_THEATRE_URL;
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
import backend.academy.shared.dto.LinkType;
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
        // Act
        TrackedLink trackedLink =
                ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(TEST_TAG), List.of(TEST_FILTER));

        // Assert
        assertThat(linkJpaRepository.findByUrl(TEST_URL)).isPresent();
        assertThat(trackedLink.url()).isEqualTo(TEST_URL);
        assertThat(trackedLink.tags().contains(TEST_TAG)).isTrue();
        assertThat(trackedLink.filters().contains(TEST_FILTER)).isTrue();
    }

    @Test
    @Transactional
    void shouldCountOnlyCanonicalPuppetTheatreUrl() {
        // Arrange
        List<String> acceptedUrls = List.of(PUPPET_THEATRE_URL);
        List<String> rejectedUrls = List.of(
                "http://puppet-minsk.by/afisha",
                "https://www.puppet-minsk.by/afisha/",
                "HTTPS://PUPPET-MINSK.BY/AFISHA?date=2026-09-12#tickets",
                "https://puppet-minsk.by/afisha/archive",
                "https://puppet-minsk.by/afishax",
                "https://notpuppet-minsk.by/afisha",
                "https://puppet-minsk.by.evil/afisha");
        acceptedUrls.forEach(url -> ormLinkRepository.addLink(TEST_CHAT_ID, url, List.of(), List.of()));
        rejectedUrls.forEach(url -> ormLinkRepository.addLink(TEST_CHAT_ID, url, List.of(), List.of()));

        // Act & Assert
        assertThat(linkJpaRepository.count()).isEqualTo((long) acceptedUrls.size() + rejectedUrls.size());
        assertThat(ormLinkRepository.countByType(LinkType.PUPPET_THEATRE)).isEqualTo((long) acceptedUrls.size());
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkAlreadyExists() {
        // Arrange
        ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        // Act & Assert
        assertThrows(
                LinkAlreadyExistsException.class,
                () -> ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of()));
    }

    @Test
    @Transactional
    void shouldRemoveLink() {
        // Arrange
        ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        // Act
        ormLinkRepository.removeLink(TEST_CHAT_ID, TEST_URL);

        // Assert
        assertThat(linkJpaRepository.findByUrl(TEST_URL)).isEmpty();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkDoesNotExist() {
        // Act & Assert
        assertThrows(LinkNotFoundException.class, () -> ormLinkRepository.removeLink(TEST_CHAT_ID, TEST_URL));
    }

    @Test
    @Transactional
    void shouldUpdateLastCheckedTime() {
        // Arrange
        TrackedLink trackedLink = ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());
        LocalDateTime lastCheckedTime = LocalDateTime.now();

        // Act
        ormLinkRepository.updateLastCheckedTime(trackedLink, lastCheckedTime);

        // Assert
        LinkEntity link = linkJpaRepository.findByUrl(TEST_URL).orElseThrow();
        assertThat(link.updatedAt()).isEqualTo(lastCheckedTime);
    }

    @Test
    @Transactional
    void shouldReturnLinksByChat() {
        // Arrange
        ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        // Act
        List<TrackedLink> links = ormLinkRepository.getLinksByChat(TEST_CHAT_ID, 0);

        // Assert
        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(TEST_URL);
    }

    @Test
    @Transactional
    void shouldReturnAllLinks() {
        // Arrange
        ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        // Act
        List<TrackedLink> links = ormLinkRepository.getAllLinks(0);

        // Assert
        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(TEST_URL);
    }

    @Test
    @Transactional
    void shouldReturnLinksByChatAndTag() {
        // Arrange
        ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(TEST_TAG), List.of());

        // Act
        List<TrackedLink> links = ormLinkRepository.getLinksByChatAndTag(TEST_CHAT_ID, TEST_TAG, 0);

        // Assert
        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(TEST_URL);
        assertThat(links.getFirst().tags()).isEqualTo(List.of(TEST_TAG));
    }

    @Test
    @Transactional
    void shouldReturnChatsForLink() {
        // Arrange
        TrackedLink trackedLink = ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        // Act
        List<Long> chats = ormLinkRepository.getChatsForLink(trackedLink, 0);

        // Assert
        assertThat(chats.size()).isEqualTo(1);
        assertThat(chats.getFirst()).isEqualTo(TEST_CHAT_ID);
    }

    @Test
    @Transactional
    void shouldReturnFiltersForChatAndLink() {
        // Arrange
        TrackedLink trackedLink = ormLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of(TEST_FILTER));

        // Act
        List<String> filters = ormLinkRepository.getFiltersForChatAndLink(TEST_CHAT_ID, trackedLink);

        // Assert
        assertThat(filters.size()).isEqualTo(1);
        assertThat(filters.contains(TEST_FILTER)).isTrue();
    }
}
