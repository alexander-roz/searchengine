package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.SiteEntity;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    SiteEntity findSiteEntityByUrlIsIgnoreCase(String url);
}
