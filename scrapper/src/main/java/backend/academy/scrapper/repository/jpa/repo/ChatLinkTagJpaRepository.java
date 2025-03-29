package backend.academy.scrapper.repository.jpa.repo;

import backend.academy.scrapper.domain.entity.ChatEntity;
import backend.academy.scrapper.domain.entity.ChatLinkTagEntity;
import backend.academy.scrapper.domain.entity.ChatLinkTagPK;
import backend.academy.scrapper.domain.entity.LinkEntity;
import backend.academy.scrapper.domain.entity.TagEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatLinkTagJpaRepository extends JpaRepository<ChatLinkTagEntity, ChatLinkTagPK> {
    List<ChatLinkTagEntity> findByLink(LinkEntity link);

    List<ChatLinkTagEntity> findByChatAndLink(ChatEntity chat, LinkEntity link);

    Optional<ChatLinkTagEntity> findByChatAndLinkAndTag(ChatEntity chat, LinkEntity link, TagEntity tag);
}
