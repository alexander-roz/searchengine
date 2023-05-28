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
import searchengine.model.repositories.SearchIndexRepository;
import searchengine.model.repositories.SiteRepository;
import searchengine.services.SearchingService;
import searchengine.services.parsing.Lemmatisation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchingServiceImpl implements SearchingService {
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository indexRepository;
    private final Lemmatisation lemmatisation;
    private final SnippetGenerator snippetGenerator;
    private final SitesList sitesList;
    private final int removableLemmasPercent = 5;

    @Override
    public SearchResponse getSearchResults(String query, String siteUrl, Integer offset, Integer limit) {
        long start = System.currentTimeMillis();
        System.out.println("Начало поиска: " + start);
        if (query.isEmpty()) {
            return new SearchResponse(false,
                    "Задан пустой поисковый запрос",
                    0,
                    new ArrayList<>());
        }

        Set<String> lemmasFromQuery = generateLemmasFromQuery(query);

        LinkedHashMap<String, Integer> lemmasSortedByFrequency = sortLemmasByFrequency(lemmasFromQuery);

        SearchResponse searchResponse = new SearchResponse();

        LinkedHashMap<LemmaEntity, PageEntity> entitiesList = new LinkedHashMap<>();

        if (siteUrl != null) {
            SiteEntity siteEntity = getSiteEntity(siteUrl);
            entitiesList = getEntitiesList(lemmasFromQuery, siteEntity, lemmasSortedByFrequency);

            LinkedHashMap<PageEntity, Integer> pagesByRelevance = countAbsoluteRank(entitiesList);
            LinkedHashMap<PageEntity, Integer> sortedPages = sortPages(pagesByRelevance);
            List<SearchData> generatedSearchDataList = generateSearchDataList(sortedPages, lemmasFromQuery, limit, offset);
            searchResponse = response(generatedSearchDataList);
        } else {
            for (Site site : sitesList.getSites()) {
                System.out.println(">>> Поиск на сайте: " + site.getName());
                SiteEntity siteEntity = getSiteEntity(site.getUrl());
                entitiesList.putAll(getEntitiesList(lemmasFromQuery, siteEntity, lemmasSortedByFrequency));
            }
            LinkedHashMap<PageEntity, Integer> pagesByRelevance = countAbsoluteRank(entitiesList);
            LinkedHashMap<PageEntity, Integer> sortedPages = sortPages(pagesByRelevance);
            List<SearchData> generatedSearchDataList = generateSearchDataList(sortedPages, lemmasFromQuery, limit, offset);
            searchResponse = response(generatedSearchDataList);
        }
        System.out.println("Окончание поиска: " + (System.currentTimeMillis() - start));
        return searchResponse;
    }

    private LinkedHashMap<LemmaEntity, PageEntity> getEntitiesList(Set<String> lemmasFromQuery,
                                                                   SiteEntity site,
                                                                   LinkedHashMap<String, Integer> lemmasSortedByFrequency) {
        List<PageEntity> pagesListFromFirstLemma =
                getPageEntityListFromFirstLemma(lemmasSortedByFrequency, site);

        List<PageEntity> pagesFilteredByNextLemmas =
                filterPagesByOtherLemmas(lemmasSortedByFrequency, pagesListFromFirstLemma);

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
                                                    int limit, int offset) {
        System.out.println(">>> Формирование списка объектов SearchData");

        if (offset != 0 && sortedPages.size() > 0) {
            sortedPages.remove(sortedPages.keySet().stream().findFirst().get());
        }

        List<SearchData> dataList = new ArrayList<>();
        int count = 0;
        for (Map.Entry<PageEntity, Integer> entry : sortedPages.entrySet()) {
            if (count < limit) {
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
        System.out.println("Сформировано " + dataList.size() + " объектов");
        return dataList;
    }

    private String shortThePath(PageEntity page, SiteEntity site) {
        String pageURL = page.getPagePath();
        String siteURL = site.getUrl();
        return pageURL.replaceAll(siteURL, "");
    }

    private SiteEntity getSiteEntity(String siteURL) {
        return siteRepository.findSiteEntityByUrlIsIgnoreCase(siteURL);
    }

    private String getSnippet(PageEntity page, Set<String> lemmas) {
        List<String> queryList = new ArrayList<>(lemmas);
        snippetGenerator.setText(page.getPageContent());
        snippetGenerator.setQueryWords(queryList);
        return snippetGenerator.generateSnippets();
    }

    private Set<String> generateLemmasFromQuery(String query) {
        return lemmatisation.getLemmas(query).keySet();
    }

    private LinkedHashMap<String, Integer> sortLemmasByFrequency(Set<String> lemmasList) {
        System.out.println(">>> Сортировка лемм по частоте встречаемости");
        LinkedHashMap<String, Integer> foundLemmas = new LinkedHashMap<>();

        for (String lemmaFromList : lemmasList) {
            AtomicInteger frequency = new AtomicInteger();
            List<LemmaEntity> lemmas = lemmaRepository.findLemmaEntitiesByLemmaEqualsIgnoreCase(lemmaFromList);

            lemmas.forEach(lemma -> System.out.println("lemma " + lemma.getLemma() + " " + lemma.getFrequency()));
            lemmas = removeMostFrequentlyLemmas(lemmas);

            lemmas.forEach(lemmaEntity -> frequency.set(frequency.get() + lemmaEntity.getFrequency()));
            foundLemmas.put(lemmaFromList, frequency.intValue());

        }

        LinkedHashMap<String, Integer> sortedMap = foundLemmas.entrySet().stream()
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

    private ArrayList<LemmaEntity> removeMostFrequentlyLemmas(List<LemmaEntity> lemmas) {
        System.out.println(">>> Исключение наиболее встречающихся лемм");
        ArrayList<LemmaEntity> reList = new ArrayList<>(lemmas);
        int removeCount = Math.round((float) lemmas.size() / 100 * removableLemmasPercent);
        System.out.println("количество исключаемых лемм = " + removeCount);
        LemmaEntity removable = new LemmaEntity();

        for (int i = 0; i < removeCount; i++) {
            int maxFrequency = 0;

            for (LemmaEntity lemma : lemmas) {
                if (lemma.getFrequency() > maxFrequency) {
                    maxFrequency = lemma.getFrequency();
                    removable = lemma;
                }
            }
            reList.remove(removable);
            System.out.println("исключена лемма: id: " +
                    removable.getLemmaID() + " " +
                    removable.getLemma() + " , frequency: " +
                    removable.getFrequency());
        }

        return reList;
    }

    private List<PageEntity> getPageEntityListFromFirstLemma(LinkedHashMap<String, Integer> sortedLemmas, SiteEntity site) {
        System.out.println(">>> Поиск страниц с самой редкой леммой из списка");
        List<PageEntity> listFromFirstLemma = new ArrayList<>();
        ArrayList<String> lemmaList = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : sortedLemmas.entrySet()) {
            lemmaList.add(entry.getKey());
        }
        String rareLemma = lemmaList.get(0);
        System.out.println("Самая редкая лемма: " + rareLemma);

        ArrayList<SearchIndex> indexesFromFirstLemma =
                indexRepository.findSearchIndicesByLemmaID_LemmaAndPageID_SiteID(rareLemma, site);
        indexesFromFirstLemma.forEach(searchIndex -> listFromFirstLemma.add(searchIndex.getPageID()));

        System.out.println("По первой лемме найдено страниц: " + listFromFirstLemma.size() + "\nсписок:");
        listFromFirstLemma.forEach(page -> System.out.println(page.getPagePath()));
        return listFromFirstLemma;
    }

    private List<PageEntity> filterPagesByOtherLemmas(LinkedHashMap<String, Integer> sortedLemmas,
                                                      List<PageEntity> pagesListFromFirstLemma) {
        System.out.println(">>> Исключение страниц, на которых отсутствуют остальные леммы");
        List<PageEntity> refactoredList = new ArrayList<>(pagesListFromFirstLemma);

        ArrayList<String> lemmaList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sortedLemmas.entrySet()) {
            lemmaList.add(entry.getKey());
        }
        if (lemmaList.size() > 0) {
            lemmaList.remove(0);

            for (PageEntity page : pagesListFromFirstLemma) {
                for (String lemma : lemmaList) {
                    if (indexRepository.findSearchIndicesByPageIDAndLemmaID_Lemma(page, lemma).isEmpty()) {
                        refactoredList.remove(page);
                        System.out.println("Исключена страница: " + page.getPagePath());
                    }
                }
            }
        }

        System.out.println("Страниц после проверки списка: " + refactoredList.size() + "\nсписок:");
        refactoredList.forEach(pageEntity -> System.out.println(pageEntity.getPagePath()));
        return refactoredList;
    }

    private LinkedHashMap<PageEntity, Integer> countAbsoluteRank(LinkedHashMap<LemmaEntity, PageEntity> lemmaAndPageList) {
        System.out.println(">>> Расчет абсолютной релевантности");

        LinkedHashMap<PageEntity, Integer> sortedList = new LinkedHashMap<>();

        for (Map.Entry<LemmaEntity, PageEntity> entry : lemmaAndPageList.entrySet()) {
            if (sortedList.containsKey(entry.getValue())) {
                int rank = sortedList.get(entry.getValue());
                sortedList.remove(entry.getValue());
                sortedList.put(entry.getValue(), (entry.getKey().getFrequency() + rank));
            } else {
                sortedList.put(entry.getValue(), entry.getKey().getFrequency());
            }
        }
        for (Map.Entry<PageEntity, Integer> entry : sortedList.entrySet()) {
            System.out.println(entry.getKey().getPagePath() + " " + entry.getValue());
        }
        return sortedList;
    }

    private LinkedHashMap<LemmaEntity, PageEntity> compareFinalPagesAndLemmas(List<PageEntity> pagesFilteredByNextLemmas,
                                                                              Set<String> lemmasFromQuery) {
        System.out.println(">>> Группировка лемм и страниц");
        LinkedHashMap<LemmaEntity, PageEntity> finalPagesAndLemmasList = new LinkedHashMap<>();

        for (PageEntity page : pagesFilteredByNextLemmas) {
            for (String lemma : lemmasFromQuery) {
                indexRepository.findSearchIndicesByPageIDAndLemmaID_Lemma(page, lemma)
                        .forEach(searchIndex ->
                                finalPagesAndLemmasList.put(searchIndex.getLemmaID(), searchIndex.getPageID()));
            }
        }

        for (Map.Entry<LemmaEntity, PageEntity> entry : finalPagesAndLemmasList.entrySet()) {
            System.out.println(entry.getKey().getLemma() + " " + entry.getValue().getPagePath());
        }
        return finalPagesAndLemmasList;
    }

    private LinkedHashMap<PageEntity, Integer> sortPages(LinkedHashMap<PageEntity, Integer> finalPages) {
        System.out.println(">>> Сортировка страниц к выдаче по релевантности");
        LinkedHashMap<PageEntity, Integer> sortedList = new LinkedHashMap<>();

        sortedList = finalPages.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new
                ));

        for (Map.Entry<PageEntity, Integer> entry : sortedList.entrySet()) {
            System.out.println(entry.getKey().getPagePath() + " " + entry.getValue());
        }
        return sortedList;
    }
}