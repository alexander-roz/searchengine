package searchengine.services.parsing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.entities.*;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SearchIndexRepository;
import searchengine.model.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PageParser {
    private SiteEntity site;
    private PageEntity page;
    private final String url;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository searchIndexRepository;
    private Document document;
    private boolean contains;

    public PageParser(String url,
                      SiteEntity site,
                      PageRepository pageRepository,
                      SiteRepository siteRepository,
                      LemmaRepository lemmaRepository,
                      SearchIndexRepository searchIndexRepository) {
        this.url = url;
        this.site = site;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.searchIndexRepository = searchIndexRepository;
    }

    public void parsePage() {

        for (PageEntity page : pageRepository.findAll()) {
            if (page.getPagePath().equalsIgnoreCase(url)) {
                this.page = page;
                contains = true;
                break;
            }
        }
        if (contains) {
            List<SearchIndex> indexList = new ArrayList<>();
            for (SearchIndex searchIndex : searchIndexRepository.findAll()) {
                if (searchIndex.getPageID().getPageID() == page.getPageID()) {
                    indexList.add(searchIndex);
                }
            }
                searchIndexRepository.deleteAll(indexList);
                pageRepository.delete(page);
        }

        Connection connection = Jsoup.connect(url)
                .ignoreContentType(true)
                .userAgent(new UserAgent().getUserAgent())
                .referrer("https://www.google.com");

        try {
            document = connection.execute().parse();
            page = new PageEntity();
            page.setSiteID(site);
            page.setPagePath(url);
            page.setPageContent(String.valueOf(document));
            page.setPageCode(connection.response().statusCode());
            pageRepository.save(page);
            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

            Lemmatisation lemmatisation = new Lemmatisation();
            Map<String, Integer> lemmas = lemmatisation.getLemmas(document.text());
            List<LemmaEntity> lemmaEntityList = new ArrayList<>();
            List<SearchIndex> searchIndexList = new ArrayList<>();

            for (Map.Entry<String, Integer> lemma : lemmas.entrySet()) {
                LemmaEntity lemmaEntity = new LemmaEntity();
                lemmaEntity.setSiteID(site);
                lemmaEntity.setLemma(lemma.getKey());
                lemmaEntity.setFrequency(lemma.getValue());
                lemmaEntityList.add(lemmaEntity);

                SearchIndex searchIndex = new SearchIndex();
                searchIndex.setPageID(page);
                searchIndex.setLemmaID(lemmaEntity);
                searchIndex.setSearchRank(lemma.getValue());
                searchIndexList.add(searchIndex);
            }
            lemmaRepository.saveAll(lemmaEntityList);
            searchIndexRepository.saveAll(searchIndexList);
        } catch (IOException e) {
            System.out.println(url + "can't be parsed");
        }
    }
}
