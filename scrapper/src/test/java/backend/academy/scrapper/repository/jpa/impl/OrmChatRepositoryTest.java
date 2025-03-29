package backend.academy.scrapper.repository.jpa.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.TestcontainersConfiguration;
import backend.academy.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.repository.jpa.repo.ChatJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.FilterJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.LinkJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.TagJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {"app.access-type=orm"})
@Import(TestcontainersConfiguration.class)
class OrmChatRepositoryTest {
    @Autowired
    private ChatJpaRepository chatJpaRepository;

    @Autowired
    private LinkJpaRepository linkJpaRepository;

    @Autowired
    private TagJpaRepository tagJpaRepository;

    @Autowired
    private FilterJpaRepository filterJpaRepository;

    private OrmChatRepository ormChatRepository;

    @BeforeEach
    void setUp() {
        ormChatRepository =
                new OrmChatRepository(chatJpaRepository, linkJpaRepository, tagJpaRepository, filterJpaRepository);
    }

    @Test
    @Transactional
    void shouldSaveChat() {
        Long chatId = 12345L;
        ormChatRepository.registerChat(chatId);

        assertThat(chatJpaRepository.findById(chatId)).isPresent();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfChatAlreadyExists() {
        Long chatId = 12345L;
        ormChatRepository.registerChat(chatId);

        assertThrows(ChatAlreadyExistsException.class, () -> ormChatRepository.registerChat(chatId));
    }

    @Test
    @Transactional
    void shouldDeleteChat() {
        Long chatId = 12345L;
        ormChatRepository.registerChat(chatId);

        ormChatRepository.deleteChat(chatId);

        assertThat(chatJpaRepository.findById(chatId)).isNotPresent();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfChatNotFound() {
        Long chatId = 99999L;

        assertThrows(ChatNotFoundException.class, () -> ormChatRepository.deleteChat(chatId));
    }
}
