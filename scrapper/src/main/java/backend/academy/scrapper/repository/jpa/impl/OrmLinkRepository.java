package backend.academy.scrapper.repository.jpa.impl;

import backend.academy.scrapper.config.ScrapperConfig;
import backend.academy.scrapper.domain.entity.ChatEntity;
import backend.academy.scrapper.domain.entity.ChatLinkFilterEntity;
import backend.academy.scrapper.domain.entity.ChatLinkTagEntity;
import backend.academy.scrapper.domain.entity.FilterEntity;
import backend.academy.scrapper.domain.entity.LinkEntity;
import backend.academy.scrapper.domain.entity.TagEntity;
import backend.academy.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.scrapper.repository.jpa.repo.ChatJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.ChatLinkFilterJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.ChatLinkTagJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.FilterJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.LinkJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.TagJpaRepository;
import backend.academy.shared.dto.LinkType;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "app.access-type", havingValue = "ORM")
public class OrmLinkRepository extends BaseOrmRepository implements LinkOperationRepository {
    private final ScrapperConfig scrapperConfig;
    private final ChatLinkFilterJpaRepository chatLinkFilterJpaRepository;
    private final ChatLinkTagJpaRepository chatLinkTagJpaRepository;

    public OrmLinkRepository(
            ChatJpaRepository chatJpaRepository,
            LinkJpaRepository linkJpaRepository,
            TagJpaRepository tagJpaRepository,
            FilterJpaRepository filterJpaRepository,
            ScrapperConfig scrapperConfig,
            ChatLinkFilterJpaRepository chatLinkFilterJpaRepository,
            ChatLinkTagJpaRepository chatLinkTagJpaRepository) {
        super(chatJpaRepository, linkJpaRepository, tagJpaRepository, filterJpaRepository);
        this.scrapperConfig = scrapperConfig;
        this.chatLinkFilterJpaRepository = chatLinkFilterJpaRepository;
        this.chatLinkTagJpaRepository = chatLinkTagJpaRepository;
    }

    @Transactional
    @Override
    public List<TrackedLink> getLinksByChat(Long chatId, int page) {
        Pageable pageable = PageRequest.of(page, scrapperConfig.batchSize(), Sort.by("id"));
        Page<LinkEntity> linkPage = linkJpaRepository.findByChatId(chatId, pageable);

        return linkPage.stream().map(this::convertToTrackedLink).toList();
    }

    @Transactional
    @Override
    public List<TrackedLink> getAllLinks(int page) {
        Pageable pageable = PageRequest.of(page, scrapperConfig.batchSize(), Sort.by("id"));
        Page<LinkEntity> linkPage = linkJpaRepository.findAll(pageable);

        return linkPage.stream().map(this::convertToTrackedLink).toList();
    }

    @Transactional
    @Override
    public List<TrackedLink> getLinksByChatAndTag(Long chatId, String tag, int page) {
        Pageable pageable = PageRequest.of(page, scrapperConfig.batchSize(), Sort.by("id"));
        Page<LinkEntity> linkPage = linkJpaRepository.findByChatIdAndTag(chatId, tag, pageable);

        return linkPage.stream().map(this::convertToTrackedLink).toList();
    }

    @Transactional
    @Override
    public List<Long> getChatsForLink(TrackedLink trackedLink, int page) {
        Pageable pageable = PageRequest.of(page, scrapperConfig.batchSize(), Sort.by("id"));
        Page<ChatEntity> chatPage = chatJpaRepository.findByLinksUrl(trackedLink.url(), pageable);

        return chatPage.stream().map(ChatEntity::id).toList();
    }

    @Transactional
    @Override
    public TrackedLink addLink(Long chatId, String url, List<String> tags, List<String> filters) {
        ChatEntity chat = getChatOrThrow(chatId);
        LinkEntity link = linkJpaRepository.findByUrl(url).orElseGet(() -> createNewLink(url));

        if (chat.links().contains(link)) {
            throw new LinkAlreadyExistsException("Ссылка уже добавлена");
        }

        chat.links().add(link);
        chatJpaRepository.save(chat);

        addTagsToLink(chat, link, tags);
        addFiltersToLink(chat, link, filters);

        return convertToTrackedLink(link);
    }

    @Transactional
    @Override
    public TrackedLink removeLink(Long chatId, String url) {
        LinkEntity link = getLinkOrThrow(url);
        ChatEntity chat = getChatOrThrow(chatId);

        if (!chat.links().contains(link)) {
            throw new LinkNotFoundException("Ссылка не отслеживается чатом");
        }

        chat.links().remove(link);
        chatJpaRepository.save(chat);
        link.chats().remove(chat);
        linkJpaRepository.save(link);

        chatLinkTagJpaRepository.deleteAll(chatLinkTagJpaRepository.findByChatAndLink(chat, link));
        chatLinkFilterJpaRepository.deleteAll(chatLinkFilterJpaRepository.findByChatAndLink(chat, link));
        tagJpaRepository.deleteAll(tagJpaRepository.findUnusedTags());
        filterJpaRepository.deleteAll(filterJpaRepository.findUnusedFilters());

        if (link.chats().isEmpty()) {
            linkJpaRepository.delete(link);
        }

        return convertToTrackedLink(link);
    }

    @Transactional
    @Override
    public void updateLastCheckedTime(TrackedLink trackedLink, LocalDateTime lastCheckedTime) {
        LinkEntity link = getLinkOrThrow(trackedLink.url());
        link.updatedAt(lastCheckedTime);
        linkJpaRepository.save(link);
    }

    @Transactional
    @Override
    public List<String> getFiltersForChatAndLink(Long chatId, TrackedLink trackedLink) {
        ChatEntity chat = getChatOrThrow(chatId);
        LinkEntity link = getLinkOrThrow(trackedLink.url());
        List<ChatLinkFilterEntity> filters = chatLinkFilterJpaRepository.findAllByChatAndLink(chat, link);

        return filters.stream()
                .map(ChatLinkFilterEntity::filter)
                .map(FilterEntity::name)
                .toList();
    }

    @Override
    public Long countByType(LinkType linkType) {
        return switch (linkType) {
            case GITHUB -> linkJpaRepository.countGithubLinks();
            case STACKOVERFLOW -> linkJpaRepository.countStackoverflowLinks();
        };
    }

    private LinkEntity createNewLink(String url) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        LinkEntity link = new LinkEntity();
        link.url(url);
        link.createdAt(now);
        link.updatedAt(now);
        return linkJpaRepository.save(link);
    }

    private void addTagsToLink(ChatEntity chat, LinkEntity link, List<String> tags) {
        tags.forEach(tagName -> {
            TagEntity tag = getOrCreateTag(tagName);
            ChatLinkTagEntity chatLinkTag = createChatLinkTag(chat, link, tag);
            chatLinkTagJpaRepository.save(chatLinkTag);
        });
    }

    private void addFiltersToLink(ChatEntity chat, LinkEntity link, List<String> filters) {
        filters.forEach(filterName -> {
            FilterEntity filter = getOrCreateFilter(filterName);
            ChatLinkFilterEntity chatLinkFilter = createChatLinkFilter(chat, link, filter);
            chatLinkFilterJpaRepository.save(chatLinkFilter);
        });
    }

    private TrackedLink convertToTrackedLink(LinkEntity link) {
        List<String> tags = chatLinkTagJpaRepository.findByLink(link).stream()
                .map(chatLinkTag -> chatLinkTag.tag().name())
                .toList();

        List<String> filters = chatLinkFilterJpaRepository.findByLink(link).stream()
                .map(chatLinkFilter -> chatLinkFilter.filter().name())
                .toList();

        return new TrackedLink(link.id(), link.url(), tags, filters, link.createdAt(), link.updatedAt());
    }
}
