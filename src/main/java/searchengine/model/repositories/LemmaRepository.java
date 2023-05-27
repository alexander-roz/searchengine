package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.LemmaEntity;

import java.util.ArrayList;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    ArrayList<LemmaEntity> findLemmaEntitiesByLemmaEqualsIgnoreCase (String lemma);
}
