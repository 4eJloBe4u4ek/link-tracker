package backend.academy.scrapper.repository.jdbc;

import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "app.access-type", havingValue = "SQL")
public class SqlLinkRepository extends BaseSqlRepository implements LinkOperationRepository {
    private final ScrapperConfig scrapperConfig;

    private static final String GET_LINKS_BY_CHAT =
            """
                SELECT links.id, links.url, links.created_at, links.updated_at
                FROM links
                JOIN chats_links ON links.id = chats_links.link_id
                WHERE chats_links.chat_id = ?
                ORDER BY links.id
                LIMIT ? OFFSET ?
            """;
    private static final String GET_ALL_LINKS =
            """
            SELECT links.id, links.url, links.created_at, links.updated_at
            FROM links
            ORDER BY links.id
            LIMIT ? OFFSET ?
            """;
    private static final String GET_LINKS_BY_CHAT_AND_TAG =
            """
            SELECT links.id, links.url, links.created_at, links.updated_at
            FROM links
            JOIN chat_link_tags ON links.id = chat_link_tags.link_id
            JOIN tags ON chat_link_tags.tag_id = tags.id
            WHERE chat_link_tags.chat_id = ? AND tags.name = ?
            LIMIT ? OFFSET ?
            """;
    private static final String GET_CHATS_BY_LINK =
            """
            SELECT chat_id FROM chats_links
            WHERE link_id = (SELECT links.id FROM links WHERE url = ?)
            ORDER BY chat_id
            LIMIT ? OFFSET ?
            """;
    private static final String ADD_LINK =
            "INSERT INTO links (url, created_at, updated_at) VALUES (?, ?, ?) RETURNING id";
    private static final String ADD_CHAT_LINK =
            "INSERT INTO chats_links (chat_id, link_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
    private static final String DELETE_CHAT_LINK = "DELETE FROM chats_links WHERE chat_id = ? AND link_id = ?";
    private static final String UPDATE_LAST_CHECKED_TIME = "UPDATE links SET updated_at = ? WHERE links.url = ?";
    private static final String GET_LINK_BY_ID = "SELECT id, url, created_at, updated_at FROM links WHERE id = ?";
    private static final String DELETE_UNUSED_LINKS_TAGS_AFTER_CHAT_UNLINK =
            "DELETE FROM chat_link_tags WHERE link_id = ? AND chat_id = ?";
    private static final String DELETE_UNUSED_LINKS_FILTERS_AFTER_CHAT_UNLINK =
            "DELETE FROM chat_link_filters WHERE link_id = ? AND chat_id = ?";
    private static final String DELETE_UNUSED_LINK =
            "DELETE FROM links WHERE id NOT IN (SELECT DISTINCT link_id FROM chats_links)";
    private static final String GET_FILTERS_BY_CHAT_AND_LINK =
            """
        SELECT f.name
        FROM chat_link_filters clf
        JOIN filters f ON clf.filter_id = f.id
        WHERE clf.chat_id = ? AND clf.link_id = (SELECT l.id FROM links l WHERE url = ?)
        """;

    private final RowMapper<TrackedLink> trackedLinkRowMapper = (rs, rowNum) -> new TrackedLink(
            rs.getLong("id"),
            rs.getString("url"),
            getTagsByLinkId(rs.getLong("id")),
            getFiltersByLinkId(rs.getLong("id")),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());

    public SqlLinkRepository(JdbcTemplate jdbcTemplate, ScrapperConfig scrapperConfig) {
        super(jdbcTemplate);
        this.scrapperConfig = scrapperConfig;
    }

    @Transactional
    @Override
    public List<TrackedLink> getLinksByChat(Long chatId, int page) {
        int offset = page * scrapperConfig.batchSize();
        return jdbcTemplate.query(GET_LINKS_BY_CHAT, trackedLinkRowMapper, chatId, scrapperConfig.batchSize(), offset);
    }

    @Transactional
    @Override
    public List<TrackedLink> getAllLinks(int page) {
        int offset = page * scrapperConfig.batchSize();
        return jdbcTemplate.query(GET_ALL_LINKS, trackedLinkRowMapper, scrapperConfig.batchSize(), offset);
    }

    @Transactional
    @Override
    public List<TrackedLink> getLinksByChatAndTag(Long chatId, String tag, int page) {
        int offset = page * scrapperConfig.batchSize();
        return jdbcTemplate.query(
                GET_LINKS_BY_CHAT_AND_TAG, trackedLinkRowMapper, chatId, tag, scrapperConfig.batchSize(), offset);
    }

    @Transactional
    @Override
    public List<Long> getChatsForLink(TrackedLink trackedLink, int page) {
        int offset = page * scrapperConfig.batchSize();
        return jdbcTemplate.queryForList(
                GET_CHATS_BY_LINK, Long.class, trackedLink.url(), scrapperConfig.batchSize(), offset);
    }

    @Transactional
    @Override
    public TrackedLink addLink(Long chatId, String url, List<String> tags, List<String> filters) {
        if (getLinkIdFromChatLink(chatId, url).isPresent()) {
            throw new LinkAlreadyExistsException("Ссылка уже добавлена");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        Long linkId = getLinkId(url).orElseGet(() -> jdbcTemplate.queryForObject(ADD_LINK, Long.class, url, now, now));
        jdbcTemplate.update(ADD_CHAT_LINK, chatId, linkId);
        tags.forEach(tag -> jdbcTemplate.update(ADD_CHAT_LINK_TAG, chatId, linkId, getOrCreateTag(tag)));
        filters.forEach(filter -> jdbcTemplate.update(ADD_CHAT_LINK_FILTER, chatId, linkId, getOrCreateFilter(filter)));

        return new TrackedLink(linkId, url, tags, filters, now, now);
    }

    @Transactional
    @Override
    public TrackedLink removeLink(Long chatId, String url) {
        Long linkId = getLinkIdFromChatLink(chatId, url)
                .orElseThrow(() -> new LinkNotFoundException("Ссылка не отслеживается чатом"));

        TrackedLink trackedLink = getLinkById(linkId);
        jdbcTemplate.update(DELETE_CHAT_LINK, chatId, linkId);
        jdbcTemplate.update(DELETE_UNUSED_LINKS_TAGS_AFTER_CHAT_UNLINK, linkId, chatId);
        jdbcTemplate.update(DELETE_UNUSED_TAGS);
        jdbcTemplate.update(DELETE_UNUSED_LINKS_FILTERS_AFTER_CHAT_UNLINK, linkId, chatId);
        jdbcTemplate.update(DELETE_UNUSED_FILTERS);
        jdbcTemplate.update(DELETE_UNUSED_LINK);
        return trackedLink;
    }

    @Transactional
    @Override
    public void updateLastCheckedTime(TrackedLink trackedLink, LocalDateTime lastCheckedTime) {
        jdbcTemplate.update(UPDATE_LAST_CHECKED_TIME, lastCheckedTime, trackedLink.url());
    }

    @Transactional
    @Override
    public List<String> getFiltersForChatAndLink(Long chatId, TrackedLink trackedLink) {
        return jdbcTemplate.queryForList(GET_FILTERS_BY_CHAT_AND_LINK, String.class, chatId, trackedLink.url());
    }

    private TrackedLink getLinkById(Long linkId) {
        return jdbcTemplate.queryForObject(GET_LINK_BY_ID, trackedLinkRowMapper, linkId);
    }
}
