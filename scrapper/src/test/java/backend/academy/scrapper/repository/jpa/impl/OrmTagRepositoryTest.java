package backend.academy.scrapper.repository.jpa.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.TestcontainersConfiguration;
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
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {"app.access-type=orm"})
@Import(TestcontainersConfiguration.class)
class OrmTagRepositoryTest {
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
        chat.id(123L);
        chat.createdAt(LocalDateTime.now());

        link = new LinkEntity();
        link.url("https://example.com");
        link.createdAt(LocalDateTime.now());
        link.updatedAt(LocalDateTime.now());
        linkJpaRepository.save(link);

        chat.links().add(link);
        chatJpaRepository.save(chat);
    }

    @Test
    @Transactional
    void shouldAddTagToLink() {
        String tagName = "tag";

        ormTagRepository.addTagToLink(chat.id(), link.url(), tagName);

        Optional<TagEntity> tag = tagJpaRepository.findByName(tagName);
        assertThat(tag).isPresent();
        assertThat(chatLinkTagJpaRepository.findByChatAndLinkAndTag(chat, link, tag.orElseThrow()))
                .isPresent();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkNotTrackedByChat() {
        ChatEntity otherChat = new ChatEntity();
        otherChat.id(999L);
        chatJpaRepository.save(otherChat);

        assertThrows(
                LinkNotFoundException.class, () -> ormTagRepository.addTagToLink(otherChat.id(), link.url(), "tag"));
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfTagAlreadyExists() {
        String tagName = "tag";
        ormTagRepository.addTagToLink(chat.id(), link.url(), tagName);

        assertThrows(
                TagAlreadyExistsException.class, () -> ormTagRepository.addTagToLink(chat.id(), link.url(), tagName));
    }

    @Test
    @Transactional
    void shouldRemoveTagFromLink() {
        String tagName = "tag";
        ormTagRepository.addTagToLink(chat.id(), link.url(), tagName);

        ormTagRepository.removeTagFromLink(chat.id(), link.url(), tagName);

        Optional<TagEntity> tag = tagJpaRepository.findByName(tagName);
        List<ChatLinkTagEntity> chatLinkTagList = chatLinkTagJpaRepository.findByChatAndLink(chat, link);
        assertThat(tag).isEmpty();
        assertThat(chatLinkTagList.isEmpty()).isTrue();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfTagDoesNotExist() {
        String tagName = "tag";
        String otherTagName = "otherTag";
        ormTagRepository.addTagToLink(chat.id(), link.url(), tagName);

        assertThrows(
                TagNotFoundException.class,
                () -> ormTagRepository.removeTagFromLink(chat.id(), link.url(), otherTagName));
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkHasNoTag() {
        String tagName = "tag";
        String otherTagName = "otherTag";
        ChatEntity otherChat = new ChatEntity();
        otherChat.id(999L);
        otherChat.createdAt(LocalDateTime.now());
        otherChat.links().add(link);
        chatJpaRepository.save(otherChat);

        ormTagRepository.addTagToLink(chat.id(), link.url(), tagName);
        ormTagRepository.addTagToLink(otherChat.id(), link.url(), otherTagName);

        assertThrows(
                TagNotFoundException.class,
                () -> ormTagRepository.removeTagFromLink(chat.id(), link.url(), otherTagName));
    }
}
