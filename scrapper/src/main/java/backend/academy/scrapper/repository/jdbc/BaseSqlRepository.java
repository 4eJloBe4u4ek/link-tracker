package backend.academy.scrapper.repository.jdbc;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public abstract class BaseSqlRepository {
    protected final JdbcTemplate jdbcTemplate;

    protected static final String CHECK_CHAT_EXISTS = "SELECT id FROM chats WHERE id = ?";
    protected static final String CHECK_LINK_EXISTS = "SELECT id FROM links WHERE url = ?";
    protected static final String CHECK_LINK_FOR_CHAT =
            "SELECT link_id FROM chats_links WHERE chat_id = ? AND link_id = (SELECT id FROM links WHERE url = ?)";

    protected static final String GET_TAG_ID_BY_NAME = "SELECT id FROM tags WHERE name = ?";
    protected static final String GET_FILTER_ID_BY_NAME = "SELECT id FROM filters WHERE name = ?";

    protected static final String ADD_TAG = "INSERT INTO tags (name) VALUES (?) RETURNING id";
    protected static final String ADD_FILTER = "INSERT INTO filters (name) VALUES (?) RETURNING id";

    protected static final String GET_TAGS_BY_LINK =
            """
                SELECT tags.name
                FROM tags
                JOIN chat_link_tags ON tags.id = chat_link_tags.tag_id
                WHERE chat_link_tags.link_id = ?
            """;
    protected static final String GET_FILTERS_BY_LINK =
            """
                SELECT filters.name
                FROM filters
                JOIN chat_link_filters ON filters.id = chat_link_filters.filter_id
                WHERE chat_link_filters.link_id = ?
            """;

    protected static final String ADD_CHAT_LINK_TAG =
            "INSERT INTO chat_link_tags (chat_id, link_id, tag_id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";
    protected static final String ADD_CHAT_LINK_FILTER =
            "INSERT INTO chat_link_filters (chat_id, link_id, filter_id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";

    protected static final String DELETE_UNUSED_TAGS =
            """
                DELETE FROM tags
                WHERE id NOT IN (SELECT DISTINCT tag_id FROM chat_link_tags);
            """;
    protected static final String DELETE_UNUSED_FILTERS =
            """
                DELETE FROM filters
                WHERE id NOT IN (SELECT DISTINCT filter_id FROM chat_link_filters);
            """;

    protected Optional<Long> queryForOptionalLong(String sql, Object... args) {
        List<Long> results = jdbcTemplate.queryForList(sql, Long.class, args);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    protected Optional<Long> getLinkId(String url) {
        return queryForOptionalLong(CHECK_LINK_EXISTS, url);
    }

    protected Optional<Long> getLinkIdFromChatLink(Long chatId, String url) {
        return queryForOptionalLong(CHECK_LINK_FOR_CHAT, chatId, url);
    }

    protected Long getOrCreateTag(String tag) {
        return queryForOptionalLong(GET_TAG_ID_BY_NAME, tag)
                .orElseGet(() -> jdbcTemplate.queryForObject(ADD_TAG, Long.class, tag));
    }

    protected Long getOrCreateFilter(String filter) {
        return queryForOptionalLong(GET_FILTER_ID_BY_NAME, filter)
                .orElseGet(() -> jdbcTemplate.queryForObject(ADD_FILTER, Long.class, filter));
    }

    protected List<String> getTagsByLinkId(Long linkId) {
        return jdbcTemplate.queryForList(GET_TAGS_BY_LINK, String.class, linkId);
    }

    protected List<String> getFiltersByLinkId(Long linkId) {
        return jdbcTemplate.queryForList(GET_FILTERS_BY_LINK, String.class, linkId);
    }

    protected boolean exists(String sql, Object... args) {
        return queryForOptionalLong(sql, args).isPresent();
    }
}
