package backend.academy.scrapper.repository.jpa.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.TestcontainersConfiguration;
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
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {"app.access-type=orm"})
@Import(TestcontainersConfiguration.class)
class OrmLinkRepositoryTest {
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
    private Long chatId;
    private String url;

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

        chatId = 123L;
        url = "https://example.com";

        ChatEntity chat = new ChatEntity();
        chat.id(chatId);
        chat.createdAt(LocalDateTime.now());
        chatJpaRepository.save(chat);
    }

    @Test
    @Transactional
    void shouldAddLink() {
        TrackedLink trackedLink = ormLinkRepository.addLink(chatId, url, List.of("tag"), List.of("filter"));

        assertThat(linkJpaRepository.findByUrl(url)).isPresent();
        assertThat(trackedLink.url()).isEqualTo(url);
        assertThat(trackedLink.tags().contains("tag")).isTrue();
        assertThat(trackedLink.filters().contains("filter")).isTrue();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkAlreadyExists() {
        ormLinkRepository.addLink(chatId, url, List.of(), List.of());

        assertThrows(
                LinkAlreadyExistsException.class, () -> ormLinkRepository.addLink(chatId, url, List.of(), List.of()));
    }

    @Test
    @Transactional
    void shouldRemoveLink() {
        ormLinkRepository.addLink(chatId, url, List.of(), List.of());

        ormLinkRepository.removeLink(chatId, url);

        assertThat(linkJpaRepository.findByUrl(url)).isEmpty();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkDoesNotExist() {
        assertThrows(LinkNotFoundException.class, () -> ormLinkRepository.removeLink(chatId, url));
    }

    @Test
    @Transactional
    void shouldUpdateLastCheckedTime() {
        TrackedLink trackedLink = ormLinkRepository.addLink(chatId, url, List.of(), List.of());
        LocalDateTime lastCheckedTime = LocalDateTime.now();

        ormLinkRepository.updateLastCheckedTime(trackedLink, lastCheckedTime);
        LinkEntity link = linkJpaRepository.findByUrl(url).orElseThrow();

        assertThat(link.updatedAt()).isEqualTo(lastCheckedTime);
    }

    @Test
    @Transactional
    void shouldReturnLinksByChat() {
        ormLinkRepository.addLink(chatId, url, List.of(), List.of());

        List<TrackedLink> links = ormLinkRepository.getLinksByChat(chatId, 0);

        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(url);
    }

    @Test
    @Transactional
    void shouldReturnAllLinks() {
        ormLinkRepository.addLink(chatId, url, List.of(), List.of());

        List<TrackedLink> links = ormLinkRepository.getAllLinks(0);

        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(url);
    }

    @Test
    @Transactional
    void shouldReturnLinksByChatAndTag() {
        String tag = "tag";
        ormLinkRepository.addLink(chatId, url, List.of(tag), List.of());

        List<TrackedLink> links = ormLinkRepository.getLinksByChatAndTag(chatId, tag, 0);

        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(url);
        assertThat(links.getFirst().tags()).isEqualTo(List.of(tag));
    }

    @Test
    @Transactional
    void shouldReturnChatsForLink() {
        TrackedLink trackedLink = ormLinkRepository.addLink(chatId, url, List.of(), List.of());

        List<Long> chats = ormLinkRepository.getChatsForLink(trackedLink, 0);

        assertThat(chats.size()).isEqualTo(1);
        assertThat(chats.getFirst()).isEqualTo(chatId);
    }
}
