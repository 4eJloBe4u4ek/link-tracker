package backend.academy.scrapper.repository.jpa.impl;

import static backend.academy.scrapper.TestData.TEST_CHAT_ID;
import static backend.academy.scrapper.TestData.TEST_FILTER;
import static backend.academy.scrapper.TestData.TEST_URL;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import backend.academy.scrapper.BaseIntegrationTest;
import backend.academy.scrapper.domain.entity.ChatEntity;
import backend.academy.scrapper.domain.entity.ChatLinkFilterEntity;
import backend.academy.scrapper.domain.entity.FilterEntity;
import backend.academy.scrapper.domain.entity.LinkEntity;
import backend.academy.scrapper.exception.FilterAlreadyExistsException;
import backend.academy.scrapper.exception.FilterNotFoundException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.repository.jpa.repo.ChatJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.ChatLinkFilterJpaRepository;
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
class OrmFilterRepositoryTest extends BaseIntegrationTest {
    @Autowired
    private ChatJpaRepository chatJpaRepository;

    @Autowired
    private LinkJpaRepository linkJpaRepository;

    @Autowired
    private TagJpaRepository tagJpaRepository;

    @Autowired
    private FilterJpaRepository filterJpaRepository;

    @Autowired
    private ChatLinkFilterJpaRepository chatLinkFilterJpaRepository;

    private OrmFilterRepository ormFilterRepository;
    private ChatEntity chat;
    private LinkEntity link;

    @BeforeEach
    void setUp() {
        ormFilterRepository = new OrmFilterRepository(
                chatJpaRepository,
                linkJpaRepository,
                tagJpaRepository,
                filterJpaRepository,
                chatLinkFilterJpaRepository);

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
    void shouldAddFilterToLink() {
        ormFilterRepository.addFilterToLink(chat.id(), link.url(), TEST_FILTER);

        Optional<FilterEntity> filter = filterJpaRepository.findByName(TEST_FILTER);
        assertThat(filter).isPresent();
        assertThat(chatLinkFilterJpaRepository.findByChatAndLinkAndFilter(chat, link, filter.orElseThrow()))
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
                LinkNotFoundException.class,
                () -> ormFilterRepository.addFilterToLink(otherChat.id(), link.url(), TEST_FILTER));
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfFilterAlreadyExists() {
        ormFilterRepository.addFilterToLink(chat.id(), link.url(), TEST_FILTER);

        assertThrows(
                FilterAlreadyExistsException.class,
                () -> ormFilterRepository.addFilterToLink(chat.id(), link.url(), TEST_FILTER));
    }

    @Test
    @Transactional
    void shouldRemoveFilterFromLink() {
        ormFilterRepository.addFilterToLink(chat.id(), link.url(), TEST_FILTER);

        ormFilterRepository.removeFilterFromLink(chat.id(), link.url(), TEST_FILTER);

        Optional<FilterEntity> filter = filterJpaRepository.findByName(TEST_FILTER);
        List<ChatLinkFilterEntity> chatLinkFilterList = chatLinkFilterJpaRepository.findByChatAndLink(chat, link);
        assertThat(filter).isEmpty();
        assertThat(chatLinkFilterList.isEmpty()).isTrue();
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfFilterDoesNotExist() {
        String otherFilterName = "otherFilter";
        ormFilterRepository.addFilterToLink(chat.id(), link.url(), TEST_FILTER);

        assertThrows(
                FilterNotFoundException.class,
                () -> ormFilterRepository.removeFilterFromLink(chat.id(), link.url(), otherFilterName));
    }

    @Test
    @Transactional
    void shouldThrowExceptionIfLinkHasNoFilter() {
        String otherFilterName = "otherFilter";
        Long otherChatId = 9999L;
        ChatEntity otherChat = new ChatEntity();
        otherChat.id(otherChatId);
        otherChat.createdAt(LocalDateTime.now());
        otherChat.links().add(link);
        chatJpaRepository.save(otherChat);

        ormFilterRepository.addFilterToLink(chat.id(), link.url(), TEST_FILTER);
        ormFilterRepository.addFilterToLink(otherChat.id(), link.url(), otherFilterName);

        assertThrows(
                FilterNotFoundException.class,
                () -> ormFilterRepository.removeFilterFromLink(chat.id(), link.url(), otherFilterName));
    }
}
