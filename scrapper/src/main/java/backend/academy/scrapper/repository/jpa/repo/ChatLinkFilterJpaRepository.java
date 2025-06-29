package backend.academy.scrapper.repository.jpa.repo;

import backend.academy.scrapper.domain.entity.ChatEntity;
import backend.academy.scrapper.domain.entity.ChatLinkFilterEntity;
import backend.academy.scrapper.domain.entity.ChatLinkFilterPK;
import backend.academy.scrapper.domain.entity.FilterEntity;
import backend.academy.scrapper.domain.entity.LinkEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatLinkFilterJpaRepository extends JpaRepository<ChatLinkFilterEntity, ChatLinkFilterPK> {
    List<ChatLinkFilterEntity> findByLink(LinkEntity link);

    List<ChatLinkFilterEntity> findByChatAndLink(ChatEntity chat, LinkEntity link);

    List<ChatLinkFilterEntity> findAllByChatAndLink(ChatEntity chat, LinkEntity link);

    Optional<ChatLinkFilterEntity> findByChatAndLinkAndFilter(ChatEntity chat, LinkEntity link, FilterEntity filter);
}
