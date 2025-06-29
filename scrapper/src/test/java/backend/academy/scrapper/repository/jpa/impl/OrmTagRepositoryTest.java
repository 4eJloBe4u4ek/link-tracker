package backend.academy.scrapper.repository.jpa.impl;

import static backend.academy.scrapper.TestData.TEST_CHAT_ID;
import static backend.academy.scrapper.TestData.TEST_TAG;
import static backend.academy.scrapper.TestData.TEST_URL;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.BaseIntegrationTest;
import backend.academy.scrapper.domain.entity.ChatEntity;
import backend.academy.scrapper.domain.entity.ChatLinkTagEntity;
import backend.academy.scrapper.domain.entity.LinkEntity;
import backend.academy.scrapper.domain.entity.TagEntity;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.exception.TagAlreadyExistsException;
import backend.academy.scrapper.exception.TagNotFoundException;
import backend.academy.scrapper.repository.jpa.repo.ChatJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.ChatLinkTagJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.FilterJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.LinkJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.TagJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {"app.access-type=orm"})
class OrmTagRepositoryTest extends BaseIntegrationTest {
    @Autowired
    private ChatJpaRepository chatJpaRepository;

    @Autowired
    private LinkJpaRepository linkJpaRepository;

    @Autowired
    private TagJpaRepository tagJpaRepository;

    @Autowired
    private FilterJpaRepository filterJpaRepository;

    @Autowired
    private ChatLinkTagJpaRepository chatLinkTagJpaRepository;

    private OrmTagRepository ormTagRepository;
    private ChatEntity chat;
    private LinkEntity link;

    @BeforeEach
    void setUp() {
        ormTagRepository = new OrmTagRepository(
                chatJpaRepository, linkJpaRepository, tagJpaRepository, filterJpaRepository, chatLinkTagJpaRepository);

        chat = new ChatEntity();
        chat.id(TEST_CHAT_ID);
        chat.createdAt(LocalDateTime.now());

        link = new LinkEntity();
        link.url(TEST_URL);
        link.createdAt(LocalDateTime.now());
        link.updatedAt(LocalDateTime.now());
        linkJpaRepository.save(link);

        chat.links().add(link);
        chatJpaRepository.save(chat);
    }

    @Test
    @Transactional
    void shouldAddTagToLink() {
        ormTagRepository.addTagToLink(chat.id(), link.url(), TEST_TAG);

        Optional<TagEntity> tag = tagJpaRepository.findByName(TEST_TAG);
        assertThat(tag).isPresent();
        assertThat(chatLinkTagJpaRepository.findByChatAndLinkAndTag(chat, link, tag.orElseThrow()))
                .isPresent();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkNotTrackedByChat() {
        ChatEntity otherChat = new ChatEntity();
        Long otherChatId = 9999L;
        otherChat.id(otherChatId);
        chatJpaRepository.save(otherChat);

        assertThrows(
                LinkNotFoundException.class, () -> ormTagRepository.addTagToLink(otherChat.id(), link.url(), TEST_TAG));
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfTagAlreadyExists() {
        ormTagRepository.addTagToLink(chat.id(), link.url(), TEST_TAG);

        assertThrows(
                TagAlreadyExistsException.class, () -> ormTagRepository.addTagToLink(chat.id(), link.url(), TEST_TAG));
    }

    @Test
    @Transactional
    void shouldRemoveTagFromLink() {
        ormTagRepository.addTagToLink(chat.id(), link.url(), TEST_TAG);

        ormTagRepository.removeTagFromLink(chat.id(), link.url(), TEST_TAG);

        Optional<TagEntity> tag = tagJpaRepository.findByName(TEST_TAG);
        List<ChatLinkTagEntity> chatLinkTagList = chatLinkTagJpaRepository.findByChatAndLink(chat, link);
        assertThat(tag).isEmpty();
        assertThat(chatLinkTagList.isEmpty()).isTrue();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfTagDoesNotExist() {
        String otherTagName = "otherTag";
        ormTagRepository.addTagToLink(chat.id(), link.url(), TEST_TAG);

        assertThrows(
                TagNotFoundException.class,
                () -> ormTagRepository.removeTagFromLink(chat.id(), link.url(), otherTagName));
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkHasNoTag() {
        String otherTagName = "otherTag";
        Long otherChatId = 9999L;
        ChatEntity otherChat = new ChatEntity();
        otherChat.id(otherChatId);
        otherChat.createdAt(LocalDateTime.now());
        otherChat.links().add(link);
        chatJpaRepository.save(otherChat);

        ormTagRepository.addTagToLink(chat.id(), link.url(), TEST_TAG);
        ormTagRepository.addTagToLink(otherChat.id(), link.url(), otherTagName);

        assertThrows(
                TagNotFoundException.class,
                () -> ormTagRepository.removeTagFromLink(chat.id(), link.url(), otherTagName));
    }
}
