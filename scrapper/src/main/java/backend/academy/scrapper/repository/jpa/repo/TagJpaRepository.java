package backend.academy.scrapper.repository.jpa.repo;

import backend.academy.scrapper.domain.entity.TagEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TagJpaRepository extends JpaRepository<TagEntity, Long> {
    Optional<TagEntity> findByName(String name);

    @Query("SELECT t FROM TagEntity t WHERE t.id NOT IN (SELECT DISTINCT clt.tag.id FROM ChatLinkTagEntity clt)")
    List<TagEntity> findUnusedTags();
}
