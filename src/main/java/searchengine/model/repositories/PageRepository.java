package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.PageEntity;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
}
