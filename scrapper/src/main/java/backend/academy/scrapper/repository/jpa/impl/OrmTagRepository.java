package backend.academy.scrapper.repository.jpa.impl;

import backend.academy.scrapper.domain.entity.ChatEntity;
import backend.academy.scrapper.domain.entity.ChatLinkTagEntity;
import backend.academy.scrapper.domain.entity.LinkEntity;
import backend.academy.scrapper.domain.entity.TagEntity;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.exception.TagAlreadyExistsException;
import backend.academy.scrapper.exception.TagNotFoundException;
import backend.academy.scrapper.repository.TagOperationRepository;
import backend.academy.scrapper.repository.jpa.repo.ChatJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.ChatLinkTagJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.FilterJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.LinkJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.TagJpaRepository;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.access-type", havingValue = "ORM")
public class OrmTagRepository extends BaseOrmRepository implements TagOperationRepository {
    private final ChatLinkTagJpaRepository chatLinkTagJpaRepository;

    public OrmTagRepository(
            ChatJpaRepository chatJpaRepository,
            LinkJpaRepository linkJpaRepository,
            TagJpaRepository tagJpaRepository,
            FilterJpaRepository filterJpaRepository,
            ChatLinkTagJpaRepository chatLinkTagJpaRepository) {
        super(chatJpaRepository, linkJpaRepository, tagJpaRepository, filterJpaRepository);
        this.chatLinkTagJpaRepository = chatLinkTagJpaRepository;
    }

    @Transactional
    @Override
    public void addTagToLink(Long chatId, String url, String tagName) {
        ChatEntity chat = getChatOrThrow(chatId);
        LinkEntity link = getLinkOrThrow(url);
        if (!chat.links().contains(link)) {
            throw new LinkNotFoundException("Ссылка не отслеживается чатом");
        }

        TagEntity tag = getOrCreateTag(tagName);
        Optional<ChatLinkTagEntity> existingTag = chatLinkTagJpaRepository.findByChatAndLinkAndTag(chat, link, tag);
        if (existingTag.isPresent()) {
            throw new TagAlreadyExistsException("Тег уже добавлен к данной ссылке");
        }

        ChatLinkTagEntity chatLinkTag = createChatLinkTag(chat, link, tag);
        chatLinkTagJpaRepository.save(chatLinkTag);
    }

    @Transactional
    @Override
    public void removeTagFromLink(Long chatId, String url, String tagName) {
        ChatEntity chat = getChatOrThrow(chatId);
        LinkEntity link = getLinkOrThrow(url);
        if (!chat.links().contains(link)) {
            throw new LinkNotFoundException("Ссылка не отслеживается чатом");
        }

        TagEntity tag =
                tagJpaRepository.findByName(tagName).orElseThrow(() -> new TagNotFoundException("Тег не найден"));

        Optional<ChatLinkTagEntity> existingTag = chatLinkTagJpaRepository.findByChatAndLinkAndTag(chat, link, tag);
        if (existingTag.isEmpty()) {
            throw new TagNotFoundException("Тег отсутствует у ссылки");
        }
        chatLinkTagJpaRepository.delete(existingTag.orElseThrow());
        tagJpaRepository.deleteAll(tagJpaRepository.findUnusedTags());
    }
}
