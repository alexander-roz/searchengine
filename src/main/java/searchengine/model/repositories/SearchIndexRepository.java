package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.LemmaEntity;
import searchengine.model.entities.PageEntity;
import searchengine.model.entities.SearchIndex;
import searchengine.model.entities.SiteEntity;

import java.util.ArrayList;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {
    ArrayList<SearchIndex> findSearchIndicesByLemmaIDAndPageID_SiteID (LemmaEntity lemma, SiteEntity site);
    ArrayList<SearchIndex> findSearchIndicesByLemmaID_LemmaAndPageID_SiteID (String lemma, SiteEntity site);
    ArrayList<SearchIndex> findSearchIndicesByPageIDAndLemmaID_Lemma (PageEntity page, String lemma);
    boolean existsByLemmaIDAndPageIDAndPageID_SiteID (LemmaEntity lemma, PageEntity page, SiteEntity site);
}
