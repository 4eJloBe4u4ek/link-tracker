package backend.academy.scrapper.repository.jpa.impl;

import backend.academy.scrapper.domain.entity.ChatEntity;
import backend.academy.scrapper.domain.entity.ChatLinkFilterEntity;
import backend.academy.scrapper.domain.entity.FilterEntity;
import backend.academy.scrapper.domain.entity.LinkEntity;
import backend.academy.scrapper.exception.FilterAlreadyExistsException;
import backend.academy.scrapper.exception.FilterNotFoundException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.repository.FilterOperationRepository;
import backend.academy.scrapper.repository.jpa.repo.ChatJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.ChatLinkFilterJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.FilterJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.LinkJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.TagJpaRepository;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.access-type", havingValue = "ORM")
public class OrmFilterRepository extends BaseOrmRepository implements FilterOperationRepository {
    private final ChatLinkFilterJpaRepository chatLinkFilterJpaRepository;

    public OrmFilterRepository(
            ChatJpaRepository chatJpaRepository,
            LinkJpaRepository linkJpaRepository,
            TagJpaRepository tagJpaRepository,
            FilterJpaRepository filterJpaRepository,
            ChatLinkFilterJpaRepository chatLinkFilterJpaRepository) {
        super(chatJpaRepository, linkJpaRepository, tagJpaRepository, filterJpaRepository);
        this.chatLinkFilterJpaRepository = chatLinkFilterJpaRepository;
    }

    @Transactional
    @Override
    public void addFilterToLink(Long chatId, String url, String filterName) {
        ChatEntity chat = getChatOrThrow(chatId);
        LinkEntity link = getLinkOrThrow(url);
        if (!chat.links().contains(link)) {
            throw new LinkNotFoundException("Ссылка не отслеживается чатом");
        }

        FilterEntity filter = getOrCreateFilter(filterName);
        Optional<ChatLinkFilterEntity> existingFilter =
                chatLinkFilterJpaRepository.findByChatAndLinkAndFilter(chat, link, filter);
        if (existingFilter.isPresent()) {
            throw new FilterAlreadyExistsException("Фильтр уже добавлен к данной ссылке");
        }

        ChatLinkFilterEntity chatLinkFilter = createChatLinkFilter(chat, link, filter);
        chatLinkFilterJpaRepository.save(chatLinkFilter);
    }

    @Transactional
    @Override
    public void removeFilterFromLink(Long chatId, String url, String filterName) {
        ChatEntity chat = getChatOrThrow(chatId);
        LinkEntity link = getLinkOrThrow(url);
        if (!chat.links().contains(link)) {
            throw new LinkNotFoundException("Ссылка не отслеживается чатом");
        }

        FilterEntity filter = filterJpaRepository
                .findByName(filterName)
                .orElseThrow(() -> new FilterNotFoundException("Фильтр не найден"));

        Optional<ChatLinkFilterEntity> existingFilter =
                chatLinkFilterJpaRepository.findByChatAndLinkAndFilter(chat, link, filter);
        if (existingFilter.isEmpty()) {
            throw new FilterNotFoundException("Фильтр отсутствует у ссылки");
        }
        chatLinkFilterJpaRepository.delete(existingFilter.orElseThrow());
        filterJpaRepository.deleteAll(filterJpaRepository.findUnusedFilters());
    }
}
