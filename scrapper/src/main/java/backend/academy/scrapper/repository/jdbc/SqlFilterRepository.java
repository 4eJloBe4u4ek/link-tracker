package backend.academy.scrapper.repository.jdbc;

import backend.academy.scrapper.exception.FilterAlreadyExistsException;
import backend.academy.scrapper.exception.FilterNotFoundException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.repository.FilterOperationRepository;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "app.access-type", havingValue = "SQL")
public class SqlFilterRepository extends BaseSqlRepository implements FilterOperationRepository {
    private static final String CHECK_FILTER_FOR_LINK =
            "SELECT filter_id FROM chat_link_filters WHERE chat_id = ? AND link_id = ? AND filter_id = ?";
    private static final String DELETE_FROM_CHAT_LINK_FILTERS =
            "DELETE FROM chat_link_filters WHERE chat_id = ? AND link_id = ? AND filter_id = ?";

    public SqlFilterRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Transactional
    @Override
    public void addFilterToLink(Long chatId, String url, String filter) {
        if (getLinkId(url).isEmpty()) {
            throw new LinkNotFoundException("Ссылка не найдена");
        }
        Long linkId = getLinkIdFromChatLink(chatId, url)
                .orElseThrow(() -> new LinkNotFoundException("Ссылка не отслеживается чатом"));

        Long filterId = getOrCreateFilter(filter);
        Optional<Long> existingFilter = queryForOptionalLong(CHECK_FILTER_FOR_LINK, chatId, linkId, filterId);
        if (existingFilter.isPresent()) {
            throw new FilterAlreadyExistsException("Фильтр уже добавлен к данной ссылке");
        }

        jdbcTemplate.update(ADD_CHAT_LINK_FILTER, chatId, linkId, filterId);
    }

    @Transactional
    @Override
    public void removeFilterFromLink(Long chatId, String url, String filter) {
        if (getLinkId(url).isEmpty()) {
            throw new LinkNotFoundException("Ссылка не найдена");
        }
        Long linkId = getLinkIdFromChatLink(chatId, url)
                .orElseThrow(() -> new LinkNotFoundException("Ссылка не отслеживается чатом"));

        Long filterId = queryForOptionalLong(GET_FILTER_ID_BY_NAME, filter)
                .orElseThrow(() -> new FilterNotFoundException("Фильтр не найден"));
        Optional<Long> existingFilter = queryForOptionalLong(CHECK_FILTER_FOR_LINK, chatId, linkId, filterId);
        if (existingFilter.isEmpty()) {
            throw new FilterNotFoundException("Фильтр отсутствует у ссылки");
        }

        jdbcTemplate.update(DELETE_FROM_CHAT_LINK_FILTERS, chatId, linkId, filterId);
        jdbcTemplate.update(DELETE_UNUSED_FILTERS);
    }
}
