package backend.academy.scrapper.repository.jpa.repo;

import backend.academy.scrapper.domain.entity.LinkEntity;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkJpaRepository extends JpaRepository<LinkEntity, Long> {
    @Override
    @NotNull
    Page<LinkEntity> findAll(@NotNull Pageable pageable);

    @Query("""
        SELECT l FROM LinkEntity l
        JOIN l.chats c WHERE c.id = :chatId
    """)
    Page<LinkEntity> findByChatId(Long chatId, Pageable pageable);

    @Query(
            """
        SELECT l FROM LinkEntity l
        JOIN ChatLinkTagEntity clt ON l.id = clt.link.id
        JOIN TagEntity t ON clt.tag.id = t.id
        WHERE clt.chat.id = :chatId AND t.name = :tag
    """)
    Page<LinkEntity> findByChatIdAndTag(Long chatId, String tag, Pageable pageable);

    Optional<LinkEntity> findByUrl(String url);

    @Query("SELECT COUNT(l) FROM LinkEntity l WHERE l.url LIKE 'https://github.com/%'")
    Long countGithubLinks();

    @Query("SELECT COUNT(l) FROM LinkEntity l WHERE l.url LIKE 'https://stackoverflow.com/questions/%'")
    Long countStackoverflowLinks();

    @Query("SELECT COUNT(l) FROM LinkEntity l "
            + "WHERE l.url LIKE 'https://www.ticketpro.by/koncertnye-ploshhadki/%/'")
    Long countTicketproLinks();

    @Query("SELECT COUNT(l) FROM LinkEntity l WHERE l.url = 'https://puppet-minsk.by/afisha'")
    Long countPuppetTheatreLinks();
}
