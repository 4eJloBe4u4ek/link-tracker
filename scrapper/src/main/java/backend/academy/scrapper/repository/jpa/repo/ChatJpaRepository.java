package backend.academy.scrapper.repository.jpa.repo;

import backend.academy.scrapper.domain.entity.ChatEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatJpaRepository extends JpaRepository<ChatEntity, Long> {
    Page<ChatEntity> findByLinksUrl(String linksUrl, Pageable pageable);
}
