package backend.academy.scrapper.repository.jdbc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.TestcontainersConfiguration;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.exception.TagAlreadyExistsException;
import backend.academy.scrapper.exception.TagNotFoundException;
import java.util.Optional;
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
class SqlTagRepositoryTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private SqlTagRepository sqlTagRepository;
    private Long chatId;
    private String url;
    private String tagName;

    @BeforeEach
    void setUp() {
        sqlTagRepository = new SqlTagRepository(jdbcTemplate);

        chatId = 12345L;
        url = "https://example.com";
        tagName = "tag";

        jdbcTemplate.update("INSERT INTO chats (id) VALUES (?)", chatId);
        jdbcTemplate.update("INSERT INTO links (url) VALUES (?)", url);
        Long linkId = jdbcTemplate.queryForObject("SELECT id FROM links WHERE url = ?", Long.class, url);
        jdbcTemplate.update("INSERT INTO chats_links (chat_id, link_id) VALUES (?, ?)", chatId, linkId);
    }

    @Test
    @Transactional
    void shouldAddTagToLink() {
        sqlTagRepository.addTagToLink(chatId, url, tagName);

        Long tagId = jdbcTemplate.queryForObject(
                "SELECT id FROM tags WHERE name = ?", (rs, rowNum) -> rs.getLong("id"), tagName);
        assertThat(tagId).isNotNull();

        Optional<Long> chatLinkTagId = jdbcTemplate
                .query(
                        "SELECT tag_id FROM chat_link_tags WHERE chat_id = ? AND link_id = (SELECT id FROM links WHERE url = ?)",
                        (rs, rowNum) -> rs.getLong("tag_id"),
                        chatId,
                        url)
                .stream()
                .findFirst();
        assertThat(chatLinkTagId).isPresent();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkNotTrackedByChat() {
        Long otherChatId = 9999L;

        assertThrows(LinkNotFoundException.class, () -> sqlTagRepository.addTagToLink(otherChatId, url, "tag"));
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfTagAlreadyExists() {
        sqlTagRepository.addTagToLink(chatId, url, tagName);

        assertThrows(TagAlreadyExistsException.class, () -> sqlTagRepository.addTagToLink(chatId, url, tagName));
    }

    @Test
    @Transactional
    void shouldRemoveTagFromLink() {
        sqlTagRepository.addTagToLink(chatId, url, tagName);
        sqlTagRepository.removeTagFromLink(chatId, url, tagName);

        Optional<Long> chatLinkTagId = jdbcTemplate
                .query(
                        "SELECT tag_id FROM chat_link_tags WHERE chat_id = ? AND link_id = (SELECT id FROM links WHERE url = ?)",
                        (rs, rowNum) -> rs.getLong("tag_id"),
                        chatId,
                        url)
                .stream()
                .findFirst();

        assertThat(chatLinkTagId).isEmpty();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfTagDoesNotExist() {
        assertThrows(
                TagNotFoundException.class, () -> sqlTagRepository.removeTagFromLink(chatId, url, "nonexistent-tag"));
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkHasNoTag() {
        sqlTagRepository.addTagToLink(chatId, url, "existing-tag");

        assertThrows(
                TagNotFoundException.class, () -> sqlTagRepository.removeTagFromLink(chatId, url, "nonexistent-tag"));
    }
}
