package backend.academy.scrapper.repository.jdbc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.TestcontainersConfiguration;
import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
class SqlLinkRepositoryTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScrapperConfig scrapperConfig;

    private SqlLinkRepository sqlLinkRepository;
    private Long chatId;
    private String url;

    @BeforeEach
    void setUp() {
        sqlLinkRepository = new SqlLinkRepository(jdbcTemplate, scrapperConfig);

        chatId = 123L;
        url = "https://example.com";
        jdbcTemplate.update("INSERT INTO chats (id, created_at) VALUES (?, ?)", chatId, LocalDateTime.now());
    }

    @Test
    @Transactional
    void shouldAddLink() {
        TrackedLink trackedLink = sqlLinkRepository.addLink(chatId, url, List.of("tag"), List.of("filter"));

        Integer countLinksWithId =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM links WHERE url = ?", Integer.class, url);
        assertThat(countLinksWithId).isEqualTo(1);
        assertThat(trackedLink.url()).isEqualTo(url);
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkAlreadyExists() {
        sqlLinkRepository.addLink(chatId, url, List.of(), List.of());

        assertThrows(
                LinkAlreadyExistsException.class, () -> sqlLinkRepository.addLink(chatId, url, List.of(), List.of()));
    }

    @Test
    @Transactional
    void shouldRemoveLink() {
        sqlLinkRepository.addLink(chatId, url, List.of(), List.of());

        sqlLinkRepository.removeLink(chatId, url);

        Integer countLinksWithId =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM links WHERE url = ?", Integer.class, url);
        assertThat(countLinksWithId).isEqualTo(0);
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkDoesNotExist() {
        assertThrows(LinkNotFoundException.class, () -> sqlLinkRepository.removeLink(chatId, url));
    }

    @Test
    @Transactional
    void shouldUpdateLastCheckedTime() {
        TrackedLink trackedLink = sqlLinkRepository.addLink(chatId, url, List.of(), List.of());
        LocalDateTime lastCheckedTime = LocalDateTime.now();

        sqlLinkRepository.updateLastCheckedTime(trackedLink, lastCheckedTime);
        LocalDateTime updateTime =
                jdbcTemplate.queryForObject("SELECT updated_at FROM links WHERE url = ?", LocalDateTime.class, url);

        assertThat(updateTime).isNotNull();
        assertThat(updateTime.truncatedTo(ChronoUnit.MILLIS)).isEqualTo(lastCheckedTime.truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    @Transactional
    void shouldReturnLinksByChat() {
        sqlLinkRepository.addLink(chatId, url, List.of(), List.of());

        List<TrackedLink> links = sqlLinkRepository.getLinksByChat(chatId, 0);

        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(url);
    }

    @Test
    @Transactional
    void shouldReturnAllLinks() {
        sqlLinkRepository.addLink(chatId, url, List.of(), List.of());

        List<TrackedLink> links = sqlLinkRepository.getAllLinks(0);

        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(url);
    }

    @Test
    @Transactional
    void shouldReturnLinksByChatAndTag() {
        String tag = "tag";
        sqlLinkRepository.addLink(chatId, url, List.of(tag), List.of());

        List<TrackedLink> links = sqlLinkRepository.getLinksByChatAndTag(chatId, tag, 0);

        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(url);
        assertThat(links.getFirst().tags()).isEqualTo(List.of(tag));
    }

    @Test
    @Transactional
    void shouldReturnChatsForLink() {
        TrackedLink trackedLink = sqlLinkRepository.addLink(chatId, url, List.of(), List.of());

        List<Long> chats = sqlLinkRepository.getChatsForLink(trackedLink, 0);

        assertThat(chats.size()).isEqualTo(1);
        assertThat(chats.getFirst()).isEqualTo(chatId);
    }
}
