package backend.academy.scrapper.repository.jpa.impl;

import backend.academy.scrapper.domain.entity.ChatEntity;
import backend.academy.scrapper.domain.entity.ChatLinkFilterEntity;
import backend.academy.scrapper.domain.entity.ChatLinkFilterPK;
import backend.academy.scrapper.domain.entity.ChatLinkTagEntity;
import backend.academy.scrapper.domain.entity.ChatLinkTagPK;
import backend.academy.scrapper.domain.entity.FilterEntity;
import backend.academy.scrapper.domain.entity.LinkEntity;
import backend.academy.scrapper.domain.entity.TagEntity;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.scrapper.repository.jpa.repo.ChatJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.FilterJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.LinkJpaRepository;
import backend.academy.scrapper.repository.jpa.repo.TagJpaRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseOrmRepository {
    protected final ChatJpaRepository chatJpaRepository;
    protected final LinkJpaRepository linkJpaRepository;
    protected final TagJpaRepository tagJpaRepository;
    protected final FilterJpaRepository filterJpaRepository;

    protected ChatEntity getChatOrThrow(Long chatId) {
        return chatJpaRepository.findById(chatId).orElseThrow(() -> new ChatNotFoundException("Чата не существует"));
    }

    protected LinkEntity getLinkOrThrow(String url) {
        return linkJpaRepository.findByUrl(url).orElseThrow(() -> new LinkNotFoundException("Ссылка не найдена"));
    }

    protected TagEntity getOrCreateTag(String tagName) {
        return tagJpaRepository.findByName(tagName).orElseGet(() -> {
            TagEntity tag = new TagEntity();
            tag.name(tagName);
            return tagJpaRepository.save(tag);
        });
    }

    protected FilterEntity getOrCreateFilter(String filterName) {
        return filterJpaRepository.findByName(filterName).orElseGet(() -> {
            FilterEntity filter = new FilterEntity();
            filter.name(filterName);
            return filterJpaRepository.save(filter);
        });
    }

    protected ChatLinkTagEntity createChatLinkTag(ChatEntity chat, LinkEntity link, TagEntity tag) {
        ChatLinkTagEntity chatLinkTag = new ChatLinkTagEntity();
        ChatLinkTagPK chatLinkTagPK = new ChatLinkTagPK();
        chatLinkTagPK.chatId(chat.id());
        chatLinkTagPK.linkId(link.id());
        chatLinkTagPK.tagId(tag.id());
        chatLinkTag.id(chatLinkTagPK);
        chatLinkTag.chat(chat);
        chatLinkTag.link(link);
        chatLinkTag.tag(tag);
        return chatLinkTag;
    }

    protected ChatLinkFilterEntity createChatLinkFilter(ChatEntity chat, LinkEntity link, FilterEntity filter) {
        ChatLinkFilterEntity chatLinkFilter = new ChatLinkFilterEntity();
        ChatLinkFilterPK chatLinkFilterPK = new ChatLinkFilterPK();
        chatLinkFilterPK.chatId(chat.id());
        chatLinkFilterPK.linkId(link.id());
        chatLinkFilterPK.filterId(filter.id());
        chatLinkFilter.id(chatLinkFilterPK);
        chatLinkFilter.chat(chat);
        chatLinkFilter.link(link);
        chatLinkFilter.filter(filter);
        return chatLinkFilter;
    }
}
