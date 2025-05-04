package backend.academy.scrapper.repository.jdbc;

import static backend.academy.scrapper.TestData.TEST_CHAT_ID;
import static backend.academy.scrapper.TestData.TEST_FILTER;
import static backend.academy.scrapper.TestData.TEST_URL;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.BaseIntegrationTest;
import backend.academy.scrapper.exception.FilterAlreadyExistsException;
import backend.academy.scrapper.exception.FilterNotFoundException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {"app.access-type=sql"})
class SqlFilterRepositoryTest extends BaseIntegrationTest {
    private static final String INSERT_CHAT = "INSERT INTO chats (id) VALUES (?)";
    private static final String INSERT_LINK = "INSERT INTO links (url) VALUES (?)";
    private static final String SELECT_LINK_ID = "SELECT id FROM links WHERE url = ?";
    private static final String INSERT_CHAT_LINK = "INSERT INTO chats_links (chat_id, link_id) VALUES (?, ?)";
    private static final String SELECT_FILTER_ID = "SELECT id FROM filters WHERE name = ?";
    private static final String SELECT_FILTER_ID_FROM_CHAT_LINK_FILTER =
            """
        SELECT filter_id FROM chat_link_filters WHERE chat_id = ?
        AND link_id = (SELECT id FROM links WHERE url = ?)
        """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private SqlFilterRepository sqlFilterRepository;

    @BeforeEach
    void setUp() {
        sqlFilterRepository = new SqlFilterRepository(jdbcTemplate);
        jdbcTemplate.update(INSERT_CHAT, TEST_CHAT_ID);
        jdbcTemplate.update(INSERT_LINK, TEST_URL);
        Long linkId = jdbcTemplate.queryForObject(SELECT_LINK_ID, Long.class, TEST_URL);
        jdbcTemplate.update(INSERT_CHAT_LINK, TEST_CHAT_ID, linkId);
    }

    @Test
    @Transactional
    void shouldAddFilterToLink() {
        sqlFilterRepository.addFilterToLink(TEST_CHAT_ID, TEST_URL, TEST_FILTER);

        Long filterId = jdbcTemplate.queryForObject(SELECT_FILTER_ID, (rs, rowNum) -> rs.getLong("id"), TEST_FILTER);
        assertThat(filterId).isNotNull();

        Optional<Long> chatLinkFilterId = jdbcTemplate
                .query(
                        SELECT_FILTER_ID_FROM_CHAT_LINK_FILTER,
                        (rs, rowNum) -> rs.getLong("filter_id"),
                        TEST_CHAT_ID,
                        TEST_URL)
                .stream()
                .findFirst();
        assertThat(chatLinkFilterId).isPresent();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkNotTrackedByChat() {
        Long otherChatId = 9999L;

        assertThrows(
                LinkNotFoundException.class,
                () -> sqlFilterRepository.addFilterToLink(otherChatId, TEST_URL, TEST_FILTER));
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfFilterAlreadyExists() {
        sqlFilterRepository.addFilterToLink(TEST_CHAT_ID, TEST_URL, TEST_FILTER);

        assertThrows(
                FilterAlreadyExistsException.class,
                () -> sqlFilterRepository.addFilterToLink(TEST_CHAT_ID, TEST_URL, TEST_FILTER));
    }

    @Test
    @Transactional
    void shouldRemoveFilterFromLink() {
        sqlFilterRepository.addFilterToLink(TEST_CHAT_ID, TEST_URL, TEST_FILTER);
        sqlFilterRepository.removeFilterFromLink(TEST_CHAT_ID, TEST_URL, TEST_FILTER);

        Optional<Long> chatLinkFilterId = jdbcTemplate
                .query(
                        SELECT_FILTER_ID_FROM_CHAT_LINK_FILTER,
                        (rs, rowNum) -> rs.getLong("filter_id"),
                        TEST_CHAT_ID,
                        TEST_URL)
                .stream()
                .findFirst();

        assertThat(chatLinkFilterId).isEmpty();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfFilterDoesNotExist() {
        assertThrows(
                FilterNotFoundException.class,
                () -> sqlFilterRepository.removeFilterFromLink(TEST_CHAT_ID, TEST_URL, "nonexistent-filter"));
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkHasNoFilter() {
        sqlFilterRepository.addFilterToLink(TEST_CHAT_ID, TEST_URL, TEST_FILTER);

        assertThrows(
                FilterNotFoundException.class,
                () -> sqlFilterRepository.removeFilterFromLink(TEST_CHAT_ID, TEST_URL, "nonexistent-filter"));
    }
}
