package backend.academy.scrapper.repository.jdbc;

import static backend.academy.scrapper.TestData.TEST_CHAT_ID;
import static backend.academy.scrapper.TestData.TEST_TAG;
import static backend.academy.scrapper.TestData.TEST_URL;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.BaseIntegrationTest;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.exception.TagAlreadyExistsException;
import backend.academy.scrapper.exception.TagNotFoundException;
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
class SqlTagRepositoryTest extends BaseIntegrationTest {
    private static final String INSERT_CHAT = "INSERT INTO chats (id) VALUES (?)";
    private static final String INSERT_LINK = "INSERT INTO links (url) VALUES (?)";
    private static final String SELECT_LINK_ID = "SELECT id FROM links WHERE url = ?";
    private static final String INSERT_CHAT_LINK = "INSERT INTO chats_links (chat_id, link_id) VALUES (?, ?)";
    private static final String SELECT_TAG_ID = "SELECT id FROM tags WHERE name = ?";
    private static final String SELECT_TAG_ID_FROM_CHAT_LINK_TAG =
            """
            SELECT tag_id FROM chat_link_tags WHERE chat_id = ?
            AND link_id = (SELECT id FROM links WHERE url = ?)
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private SqlTagRepository sqlTagRepository;

    @BeforeEach
    void setUp() {
        sqlTagRepository = new SqlTagRepository(jdbcTemplate);
        jdbcTemplate.update(INSERT_CHAT, TEST_CHAT_ID);
        jdbcTemplate.update(INSERT_LINK, TEST_URL);
        Long linkId = jdbcTemplate.queryForObject(SELECT_LINK_ID, Long.class, TEST_URL);
        jdbcTemplate.update(INSERT_CHAT_LINK, TEST_CHAT_ID, linkId);
    }

    @Test
    @Transactional
    void shouldAddTagToLink() {
        sqlTagRepository.addTagToLink(TEST_CHAT_ID, TEST_URL, TEST_TAG);

        Long tagId = jdbcTemplate.queryForObject(SELECT_TAG_ID, (rs, rowNum) -> rs.getLong("id"), TEST_TAG);
        assertThat(tagId).isNotNull();

        Optional<Long> chatLinkTagId = jdbcTemplate
                .query(SELECT_TAG_ID_FROM_CHAT_LINK_TAG, (rs, rowNum) -> rs.getLong("tag_id"), TEST_CHAT_ID, TEST_URL)
                .stream()
                .findFirst();
        assertThat(chatLinkTagId).isPresent();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkNotTrackedByChat() {
        Long otherChatId = 9999L;

        assertThrows(LinkNotFoundException.class, () -> sqlTagRepository.addTagToLink(otherChatId, TEST_URL, TEST_TAG));
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfTagAlreadyExists() {
        sqlTagRepository.addTagToLink(TEST_CHAT_ID, TEST_URL, TEST_TAG);

        assertThrows(
                TagAlreadyExistsException.class, () -> sqlTagRepository.addTagToLink(TEST_CHAT_ID, TEST_URL, TEST_TAG));
    }

    @Test
    @Transactional
    void shouldRemoveTagFromLink() {
        sqlTagRepository.addTagToLink(TEST_CHAT_ID, TEST_URL, TEST_TAG);
        sqlTagRepository.removeTagFromLink(TEST_CHAT_ID, TEST_URL, TEST_TAG);

        Optional<Long> chatLinkTagId = jdbcTemplate
                .query(SELECT_TAG_ID_FROM_CHAT_LINK_TAG, (rs, rowNum) -> rs.getLong("tag_id"), TEST_CHAT_ID, TEST_URL)
                .stream()
                .findFirst();

        assertThat(chatLinkTagId).isEmpty();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfTagDoesNotExist() {
        assertThrows(
                TagNotFoundException.class,
                () -> sqlTagRepository.removeTagFromLink(TEST_CHAT_ID, TEST_URL, "nonexistent-tag"));
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkHasNoTag() {
        sqlTagRepository.addTagToLink(TEST_CHAT_ID, TEST_URL, TEST_TAG);

        assertThrows(
                TagNotFoundException.class,
                () -> sqlTagRepository.removeTagFromLink(TEST_CHAT_ID, TEST_URL, "nonexistent-tag"));
    }
}
