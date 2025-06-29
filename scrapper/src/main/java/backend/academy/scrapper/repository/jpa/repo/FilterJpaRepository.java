package backend.academy.scrapper.repository.jpa.repo;

import backend.academy.scrapper.domain.entity.FilterEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FilterJpaRepository extends JpaRepository<FilterEntity, Long> {
    Optional<FilterEntity> findByName(String name);

    @Query(
            "SELECT f FROM FilterEntity f WHERE f.id NOT IN (SELECT DISTINCT clf.filter.id FROM ChatLinkFilterEntity clf)")
    List<FilterEntity> findUnusedFilters();
}
