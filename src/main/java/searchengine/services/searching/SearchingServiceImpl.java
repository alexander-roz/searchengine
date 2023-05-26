package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.searching.SearchData;
import searchengine.dto.searching.SearchResponse;
import searchengine.model.entities.LemmaEntity;
import searchengine.model.entities.PageEntity;
import searchengine.model.entities.SearchIndex;
import searchengine.model.entities.SiteEntity;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SearchIndexRepository;
import searchengine.model.repositories.SiteRepository;
import searchengine.services.SearchingService;
import searchengine.services.parsing.Lemmatisation;

import javax.swing.text.Document;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchingServiceImpl implements SearchingService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository indexRepository;
    private final Lemmatisation lemmatisation;
    private final SnippetGenerator snippetGenerator;
    private final SitesList sitesList;

    @Override
    public SearchResponse getSearchResults(String query, String siteUrl, Integer offset, Integer limit) {
        if (query.isEmpty()) {
            return new SearchResponse(false,
                    "Задан пустой поисковый запрос",
                    0,
                    new ArrayList<>());
        }

        Set<String> lemmasFromQuery = generateLemmasFromQuery(query);

        Map<String, Integer> lemmasSortedByFrequency = sortLemmasByFrequency(lemmasFromQuery);

        SearchResponse searchResponse = new SearchResponse();
        LinkedHashMap<LemmaEntity, PageEntity> entitiesList = new LinkedHashMap<>();

        if (siteUrl != null) {
            SiteEntity siteEntity = getSiteEntity(siteUrl);
            entitiesList = getEntitiesList(lemmasFromQuery, siteEntity, lemmasSortedByFrequency);
            LinkedHashMap<PageEntity, Integer> pagesByRelevance = sortPagesByRelevance(entitiesList);
            LinkedHashMap<PageEntity, Integer> sortedPages = sortPages(pagesByRelevance);
            List<SearchData> generatedSearchDataList = generateSearchDataList(sortedPages, lemmasFromQuery, limit, offset);
            searchResponse = response(generatedSearchDataList);
        }
        else {
            for(Site site:sitesList.getSites()){
                SiteEntity siteEntity = getSiteEntity(site.getUrl());
                entitiesList.putAll(getEntitiesList(lemmasFromQuery, siteEntity, lemmasSortedByFrequency));
            }
            LinkedHashMap<PageEntity, Integer> pagesByRelevance = sortPagesByRelevance(entitiesList);
            LinkedHashMap<PageEntity, Integer> sortedPages = sortPages(pagesByRelevance);
            List<SearchData> generatedSearchDataList = generateSearchDataList(sortedPages, lemmasFromQuery, limit, offset);
            searchResponse = response(generatedSearchDataList);
        }
        return searchResponse;
    }

    private LinkedHashMap<LemmaEntity, PageEntity> getEntitiesList(Set<String> lemmasFromQuery,
                                              SiteEntity site,
                                              Map<String, Integer> lemmasSortedByFrequency){
        List<PageEntity> pagesListFromFirstLemma =
                getPageEntityListFromFirstLemma((LinkedHashMap<String, Integer>) lemmasSortedByFrequency, site);

        List<PageEntity> pagesFilteredByNextLemmas = filterPagesByOtherLemmas(
                (LinkedHashMap<String, Integer>) lemmasSortedByFrequency, pagesListFromFirstLemma, site);

        LinkedHashMap<LemmaEntity, PageEntity> finalPagesAndLemma = compareFinalPagesAndLemmas(
                pagesFilteredByNextLemmas, lemmasFromQuery);
        return finalPagesAndLemma;
    }

    private SearchResponse response(List<SearchData> searchData) {
        return SearchResponse.builder()
                .result(true)
                .count(searchData.size())
                .data(searchData)
                .build();
    }

    private SearchData generateSearchData(String site,
                                          String siteName,
                                          String uri,
                                          String title,
                                          String snippet,
                                          float relevance) {
        return SearchData.builder()
                .site(site)
                .siteName(siteName)
                .uri(uri)
                .title(title)
                .snippet(snippet)
                .relevance(relevance)
                .build();
    }

    private List<SearchData> generateSearchDataList(LinkedHashMap<PageEntity,
                                                    Integer> sortedPages,
                                                    Set<String> lemmasFromQuery,
                                                    int limit, int offset){
        System.out.println("Формирование объектов SearchData для выдачи в Request");

        if(offset != 0){
            sortedPages.remove(sortedPages.keySet().stream().findFirst().get());
        }

        List<SearchData> dataList = new ArrayList<>();
        int count = 0;
        for(Map.Entry<PageEntity, Integer> entry:sortedPages.entrySet()){
            if(count < limit) {
                dataList.add(
                        generateSearchData(
                                entry.getKey().getSiteID().getUrl(),
                                entry.getKey().getSiteID().getName(),
                                shortThePath(entry.getKey(), entry.getKey().getSiteID()),
                                Jsoup.parse(entry.getKey().getPageContent()).title(),
                                getSnippet(entry.getKey(), lemmasFromQuery),
                                entry.getValue())
                );
                count++;
            }
        }
        return dataList;
    }

    private String shortThePath(PageEntity page, SiteEntity site){
        String pageURL = page.getPagePath();
        String siteURL = site.getUrl();
        return pageURL.replaceAll(siteURL, "");
    }

    private SiteEntity getSiteEntity(String siteURL){
        SiteEntity siteEntity = new SiteEntity();
        for(SiteEntity site: siteRepository.findAll()){
            if(site.getUrl().equals(siteURL)){
                siteEntity = site;
            }
        }
        return siteEntity;
    }

    private String getSnippet(PageEntity page, Set<String> lemmas){
        List<String> queryList = new ArrayList<>(lemmas);
        snippetGenerator.setText(page.getPageContent());
        snippetGenerator.setQueryWords(queryList);
        return snippetGenerator.generateSnippets();
    }

    private Set<String> generateLemmasFromQuery(String query) {
        return lemmatisation.getLemmas(query).keySet();
    }
    private Map<String, Integer> sortLemmasByFrequency(Set<String> lemmasList) {
        System.out.println("Сортировка лемм по частоте встречаемости");
        Map<String, Integer> foundLemmas = new LinkedHashMap<>();

        for (String lemmaFromList : lemmasList) {
            int frequancy = 0;
            for (LemmaEntity lemmaEntity : lemmaRepository.findAll()) {
                if (lemmaEntity.getLemma().equalsIgnoreCase(lemmaFromList)) {
                    frequancy = frequancy + lemmaEntity.getFrequency();
                }
            }
            foundLemmas.put(lemmaFromList, frequancy);
        }
        Map<String, Integer> sortedMap = foundLemmas.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new
                ));
        sortedMap.forEach((s, integer) -> System.out.println(s + " " + integer));
        return sortedMap;
    }

    private List<PageEntity> getPageEntityListFromFirstLemma(LinkedHashMap<String, Integer> sortedLemmas, SiteEntity site) {
        System.out.println("Поиск страниц с самой редкой леммой из списка");
        int count = 0;
        List<PageEntity> listFromFirstLemma = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : sortedLemmas.entrySet()) {

            if (count == 0) {
                List<LemmaEntity> lemmaEntityList = new ArrayList<>();
                for (LemmaEntity l : lemmaRepository.findAll()) {
                    if (l.getLemma().equals(entry.getKey())) {
                        lemmaEntityList.add(l);
                        System.out.println(l.getLemma());
                    }
                }
                for (LemmaEntity lemma : lemmaEntityList) {
                    for (SearchIndex index : indexRepository.findAll()) {
                        if (index.getLemmaID().getLemmaID() == (lemma.getLemmaID())) {
                            for (PageEntity page : pageRepository.findAll()) {
                                if (page.getPageID() == index.getPageID().getPageID() && page.getSiteID().equals(site)) {
                                    listFromFirstLemma.add(page);
                                }
                            }
                        }
                    }
                }
            }
            listFromFirstLemma.forEach(pageEntity -> System.out.println(pageEntity.getPagePath()));
            count++;
        }
        return listFromFirstLemma;
    }

    private List<PageEntity> filterPagesByOtherLemmas(LinkedHashMap<String, Integer> sortedLemmas,
                                                      List<PageEntity> pagesListFromFirstLemma, SiteEntity site) {
        System.out.println("Исключение страниц, на которых отсутствуют остальные леммы");
        List<PageEntity> refactoredList = new ArrayList<>(pagesListFromFirstLemma);

        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedLemmas.entrySet()) {
            if (count > 0) {
                List<LemmaEntity> nextLemmaEntityList = new ArrayList<>();
                for (LemmaEntity l : lemmaRepository.findAll()) {
                    if (l.getLemma().equals(entry.getKey())) {
                        nextLemmaEntityList.add(l);
                    }
                }
                nextLemmaEntityList.forEach(lemmaEntity -> System.out.println(lemmaEntity.getLemma()));

                for(PageEntity page:pagesListFromFirstLemma){
                    boolean contains = false;
                    for(SearchIndex index:indexRepository.findAll()){
                        for(LemmaEntity lemma:nextLemmaEntityList){
                            if(index.getLemmaID().getLemmaID()==(lemma.getLemmaID())){
                                if(index.getPageID().getPageID() == page.getPageID() && page.getSiteID().equals(site)){
                                    contains = true;
                                }
                            }
                        }
                    }
                    if(!contains){
                        refactoredList.remove(page);
                        System.out.println("Исключена страница " + page.getPagePath());
                    }
                }
            }
            count++;
        }
        System.out.println("Финальный список");
        refactoredList.forEach(pageEntity -> System.out.println(pageEntity.getPagePath()));
        return refactoredList;
    }

    private LinkedHashMap<PageEntity, Integer> sortPagesByRelevance(LinkedHashMap<LemmaEntity, PageEntity> lemmaAndPageList){
        System.out.println("Сортировка страниц по релевантности лемм");

        LinkedHashMap<PageEntity, Integer> sortedList = new LinkedHashMap<>();

        for(Map.Entry<LemmaEntity, PageEntity> entry:lemmaAndPageList.entrySet()){
            if(sortedList.containsKey(entry.getValue()))
            {
               int rank = sortedList.get(entry.getValue());
               sortedList.remove(entry.getValue());
               sortedList.put(entry.getValue(), (entry.getKey().getFrequency() + rank));
            }
            else{
                sortedList.put(entry.getValue(), entry.getKey().getFrequency());
            }
        }
        for(Map.Entry<PageEntity, Integer> entry: sortedList.entrySet()){
            System.out.println(entry.getKey().getPagePath() + " " + entry.getValue());
        }
        return sortedList;
    }

    private LinkedHashMap<LemmaEntity, PageEntity> compareFinalPagesAndLemmas(List<PageEntity> pagesFilteredByNextLemmas,
                                                                              Set<String> lemmasFromQuery) {
        System.out.println("Группировка лемм и страниц");
        LinkedHashMap<LemmaEntity, PageEntity> finalPagesAndLemmasList = new LinkedHashMap<>();
        List<LemmaEntity> lemmaEntityList = new ArrayList<>();

        for(String lemma:lemmasFromQuery) {
            for (LemmaEntity lemmaEntity : lemmaRepository.findAll()) {
                if (lemmaEntity.getLemma().equalsIgnoreCase(lemma)) {
                    lemmaEntityList.add(lemmaEntity);
                }
            }
        }

        for(PageEntity page:pagesFilteredByNextLemmas){
            for(SearchIndex searchIndex:indexRepository.findAll()){
                for(LemmaEntity lemma:lemmaEntityList){
                    if(searchIndex.getPageID().getPageID() == page.getPageID()
                            && searchIndex.getLemmaID().getLemmaID() == lemma.getLemmaID()){
                        finalPagesAndLemmasList.put(lemma,page);
                    }
                }
            }
        }
        for(Map.Entry<LemmaEntity, PageEntity> entry:finalPagesAndLemmasList.entrySet()){
            System.out.println(entry.getKey().getLemma() + " " + entry.getValue().getPagePath());
        }
        return finalPagesAndLemmasList;
    }

    private LinkedHashMap<PageEntity, Integer> sortPages(LinkedHashMap<PageEntity, Integer> finalPages){
        System.out.println("Сортировка страниц к выдаче по релевантности");
        LinkedHashMap<PageEntity, Integer> sortedList = new LinkedHashMap<>();

        sortedList = finalPages.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> { throw new AssertionError(); },
                        LinkedHashMap::new
                ));

        for(Map.Entry<PageEntity, Integer> entry: sortedList.entrySet()){
            System.out.println(entry.getKey().getPagePath() + " " + entry.getValue());
        }
        return sortedList;
    }
}