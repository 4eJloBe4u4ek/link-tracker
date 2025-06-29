package backend.academy.scrapper.repository.jdbc;

import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.exception.TagAlreadyExistsException;
import backend.academy.scrapper.exception.TagNotFoundException;
import backend.academy.scrapper.repository.TagOperationRepository;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.access-type", havingValue = "SQL")
public class SqlTagRepository extends BaseSqlRepository implements TagOperationRepository {
    private static final String CHECK_TAG_FOR_LINK =
            "SELECT tag_id FROM chat_link_tags WHERE chat_id = ? AND link_id = ? AND tag_id = ?";
    private static final String DELETE_FROM_CHAT_LINK_TAGS =
            "DELETE FROM chat_link_tags WHERE chat_id = ? AND link_id = ? AND tag_id = ?";

    public SqlTagRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public void addTagToLink(Long chatId, String url, String tag) {
        if (getLinkId(url).isEmpty()) {
            throw new LinkNotFoundException("Ссылка не найдена");
        }
        Long linkId = getLinkIdFromChatLink(chatId, url)
                .orElseThrow(() -> new LinkNotFoundException("Ссылка не отслеживается чатом"));

        Long tagId = getOrCreateTag(tag);
        Optional<Long> existingTag = queryForOptionalLong(CHECK_TAG_FOR_LINK, chatId, linkId, tagId);
        if (existingTag.isPresent()) {
            throw new TagAlreadyExistsException("Тег уже добавлен к данной ссылке");
        }

        jdbcTemplate.update(ADD_CHAT_LINK_TAG, chatId, linkId, tagId);
    }

    @Override
    public void removeTagFromLink(Long chatId, String url, String tag) {
        if (getLinkId(url).isEmpty()) {
            throw new LinkNotFoundException("Ссылка не найдена");
        }
        Long linkId = getLinkIdFromChatLink(chatId, url)
                .orElseThrow(() -> new LinkNotFoundException("Ссылка не отслеживается чатом"));

        Long tagId = queryForOptionalLong(GET_TAG_ID_BY_NAME, tag)
                .orElseThrow(() -> new TagNotFoundException("Тег не найден"));
        Optional<Long> existingTag = queryForOptionalLong(CHECK_TAG_FOR_LINK, chatId, linkId, tagId);
        if (existingTag.isEmpty()) {
            throw new TagNotFoundException("Тег отсутствует у ссылки");
        }

        jdbcTemplate.update(DELETE_FROM_CHAT_LINK_TAGS, chatId, linkId, tagId);
        jdbcTemplate.update(DELETE_UNUSED_TAGS);
    }
}
