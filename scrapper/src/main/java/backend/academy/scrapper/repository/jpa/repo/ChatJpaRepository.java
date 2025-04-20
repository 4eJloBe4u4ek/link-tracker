package backend.academy.scrapper.repository.jpa.repo;

import backend.academy.scrapper.domain.entity.ChatEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatJpaRepository extends JpaRepository<ChatEntity, Long> {
    Page<ChatEntity> findByLinksUrl(String linksUrl, Pageable pageable);

    @Query(
            value =
                    """
    SELECT c.id
    FROM chats c
    WHERE c.notification_mode = 'DAILY_DIGEST'
      AND c.digest_time = date_trunc('minute', now()::time)
""",
            nativeQuery = true)
    List<Long> findChatIdsWithDigestTimeMatchingNow();
}
