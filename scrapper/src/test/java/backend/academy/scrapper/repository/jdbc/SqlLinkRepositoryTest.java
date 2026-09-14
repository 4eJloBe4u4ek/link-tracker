package backend.academy.scrapper.repository.jdbc;

import static backend.academy.scrapper.TestData.TEST_CHAT_ID;
import static backend.academy.scrapper.TestData.TEST_FILTER;
import static backend.academy.scrapper.TestData.TEST_TAG;
import static backend.academy.scrapper.TestData.TEST_URL;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.BaseIntegrationTest;
import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.shared.dto.LinkType;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
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
class SqlLinkRepositoryTest extends BaseIntegrationTest {
    private static final String INSERT_CHAT = "INSERT INTO chats (id, created_at) VALUES (?, ?)";
    private static final String COUNT_LINKS_BY_URL = "SELECT COUNT(*) FROM links WHERE url = ?";
    private static final String GET_UPDATED_AT_BY_URL = "SELECT updated_at FROM links WHERE url = ?";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScrapperConfig scrapperConfig;

    private SqlLinkRepository sqlLinkRepository;

    @BeforeEach
    void setUp() {
        sqlLinkRepository = new SqlLinkRepository(jdbcTemplate, scrapperConfig);

        jdbcTemplate.update(INSERT_CHAT, TEST_CHAT_ID, LocalDateTime.now());
    }

    @Test
    @Transactional
    void shouldAddLink() {
        // Act
        TrackedLink trackedLink = sqlLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        // Assert
        Integer countLinksWithId = jdbcTemplate.queryForObject(COUNT_LINKS_BY_URL, Integer.class, TEST_URL);
        assertThat(countLinksWithId).isEqualTo(1);
        assertThat(trackedLink.url()).isEqualTo(TEST_URL);
    }

    @Test
    @Transactional
    void shouldCountTicketproVenueLinks() {
        // Arrange
        List<String> ticketproVenueUrls = List.of(
                "https://www.ticketpro.by/koncertnye-ploshhadki/belorusskij-gosudarstvennyj-teatr-kukol/",
                "https://www.ticketpro.by/koncertnye-ploshhadki/dvorec-respubliki/",
                "https://www.ticketpro.by/koncertnye-ploshhadki/kz-minsk/");
        List<String> unrelatedUrls = List.of(
                "https://github.com/spring-projects/spring-boot",
                "https://puppet-minsk.by/afisha");
        ticketproVenueUrls.forEach(url -> sqlLinkRepository.addLink(TEST_CHAT_ID, url, List.of(), List.of()));
        unrelatedUrls.forEach(url -> sqlLinkRepository.addLink(TEST_CHAT_ID, url, List.of(), List.of()));

        // Act
        Long ticketproLinkCount = sqlLinkRepository.countByType(LinkType.TICKETPRO);

        // Assert
        assertThat(ticketproLinkCount).isEqualTo(3L);
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkAlreadyExists() {
        // Arrange
        sqlLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        // Act & Assert
        assertThrows(
                LinkAlreadyExistsException.class,
                () -> sqlLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of()));
    }

    @Test
    @Transactional
    void shouldRemoveLink() {
        // Arrange
        sqlLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        // Act
        sqlLinkRepository.removeLink(TEST_CHAT_ID, TEST_URL);

        // Assert
        Integer countLinksWithId = jdbcTemplate.queryForObject(COUNT_LINKS_BY_URL, Integer.class, TEST_URL);
        assertThat(countLinksWithId).isEqualTo(0);
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkDoesNotExist() {
        // Act & Assert
        assertThrows(LinkNotFoundException.class, () -> sqlLinkRepository.removeLink(TEST_CHAT_ID, TEST_URL));
    }

    @Test
    @Transactional
    void shouldUpdateLastCheckedTime() {
        // Arrange
        TrackedLink trackedLink = sqlLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());
        LocalDateTime lastCheckedTime = LocalDateTime.now();

        // Act
        sqlLinkRepository.updateLastCheckedTime(trackedLink, lastCheckedTime);

        // Assert
        LocalDateTime updateTime = jdbcTemplate.queryForObject(GET_UPDATED_AT_BY_URL, LocalDateTime.class, TEST_URL);
        assertThat(updateTime).isNotNull();
        assertThat(updateTime.withNano(0)).isEqualTo(lastCheckedTime.withNano(0));
    }

    @Test
    @Transactional
    void shouldReturnLinksByChat() {
        // Arrange
        sqlLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        // Act
        List<TrackedLink> links = sqlLinkRepository.getLinksByChat(TEST_CHAT_ID, 0);

        // Assert
        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(TEST_URL);
    }

    @Test
    @Transactional
    void shouldReturnAllLinks() {
        // Arrange
        sqlLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        // Act
        List<TrackedLink> links = sqlLinkRepository.getAllLinks(0);

        // Assert
        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(TEST_URL);
    }

    @Test
    @Transactional
    void shouldReturnLinksByChatAndTag() {
        // Arrange
        sqlLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(TEST_TAG), List.of());

        // Act
        List<TrackedLink> links = sqlLinkRepository.getLinksByChatAndTag(TEST_CHAT_ID, TEST_TAG, 0);

        // Assert
        assertThat(links.size()).isEqualTo(1);
        assertThat(links.getFirst().url()).isEqualTo(TEST_URL);
        assertThat(links.getFirst().tags()).isEqualTo(List.of(TEST_TAG));
    }

    @Test
    @Transactional
    void shouldReturnChatsForLink() {
        // Arrange
        TrackedLink trackedLink = sqlLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of());

        // Act
        List<Long> chats = sqlLinkRepository.getChatsForLink(trackedLink, 0);

        // Assert
        assertThat(chats.size()).isEqualTo(1);
        assertThat(chats.getFirst()).isEqualTo(TEST_CHAT_ID);
    }

    @Test
    @Transactional
    void shouldReturnFiltersForChatAndLink() {
        // Arrange
        TrackedLink trackedLink = sqlLinkRepository.addLink(TEST_CHAT_ID, TEST_URL, List.of(), List.of(TEST_FILTER));

        // Act
        List<String> filters = sqlLinkRepository.getFiltersForChatAndLink(TEST_CHAT_ID, trackedLink);

        // Assert
        assertThat(filters.size()).isEqualTo(1);
        assertThat(filters.contains(TEST_FILTER)).isTrue();
    }
}
