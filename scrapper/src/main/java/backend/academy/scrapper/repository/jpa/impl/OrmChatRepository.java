package backend.academy.scrapper.repository.jpa.impl;

import backend.academy.scrapper.domain.entity.ChatEntity;
import backend.academy.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.repository.ChatOperationRepository;
import backend.academy.scrapper.repository.jpa.repo.ChatJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.FilterJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.LinkJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.TagJpaRepository;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "app.access-type", havingValue = "ORM")
public class OrmChatRepository extends BaseOrmRepository implements ChatOperationRepository {
    public OrmChatRepository(
            ChatJpaRepository chatJpaRepository,
            LinkJpaRepository linkJpaRepository,
            TagJpaRepository tagJpaRepository,
            FilterJpaRepository filterJpaRepository) {
        super(chatJpaRepository, linkJpaRepository, tagJpaRepository, filterJpaRepository);
    }

    @Transactional
    @Override
    public void registerChat(Long chatId) {
        if (chatJpaRepository.existsById(chatId)) {
            throw new ChatAlreadyExistsException("Чат уже существует");
        }

        ChatEntity chat = new ChatEntity();
        chat.id(chatId);
        chat.createdAt(LocalDateTime.now());
        chatJpaRepository.save(chat);
    }

    @Transactional
    @Override
    public void deleteChat(Long chatId) {
        if (!chatJpaRepository.existsById(chatId)) {
            throw new ChatNotFoundException("Чата не существует");
        }

        chatJpaRepository.deleteById(chatId);
    }
}
