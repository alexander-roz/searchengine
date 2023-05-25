package searchengine.services.parsing;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.entities.*;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SearchIndexRepository;
import searchengine.model.repositories.SiteRepository;
import searchengine.services.ParsingService;

import java.time.LocalDateTime;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

@Service
public class ParsingServiceImpl implements ParsingService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository searchIndexRepository;
    private boolean started;
    private boolean contains;
    private SiteEntity siteEntity;
    private ForkJoinPool forkJoinPool;

    public ParsingServiceImpl(SitesList sites,
                              SiteRepository siteRepository,
                              PageRepository pageRepository,
                              LemmaRepository lemmaRepository,
                              SearchIndexRepository searchIndexRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.searchIndexRepository = searchIndexRepository;
    }

    @Override
    @SneakyThrows
    public IndexingResponse startIndexing() {
        if (started) {
            return new IndexingResponse(false, "Индексация уже запущена");
        } else {
            started = true;
        }
        searchIndexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();

        for (Site site : sites.getSites()) {
            indexSite(site);
        }
        started = false;
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!started) {
            return new IndexingResponse(false, "Индексация не запущена");
        } else {
            started = false;
        }
        forkJoinPool.shutdownNow();
        Iterable<SiteEntity> siteList = siteRepository.findAll();
        for (SiteEntity site : siteList) {
            if (site.getStatus() == Status.INDEXING) {
                site.setStatus(Status.FAILED);
                site.setStatusTime(LocalDateTime.now());
                site.setLastError("Процесс индексации остановлен");
                siteRepository.save(site);
            }
        }
        return new IndexingResponse(true);
    }

    @Override
    @SneakyThrows
    public IndexingResponse indexPage(String url) {
        Site indexingSite = new Site();
        for(Site site:sites.getSites()) {
            if(url.contains(site.getUrl())) {
                contains = true;
                indexingSite = site;
                break;
            }
        }
        if(contains){
            boolean containsInRepository = false;
            for(SiteEntity site:siteRepository.findAll()){
                if(site.getName().equals(indexingSite.getName())){
                    siteEntity = site;
                    System.out.println("Сайт был найден в репозитории. Запущена переиндексация страницы");
                    containsInRepository = true;
                    break;
                }
            }

            if(!containsInRepository){
                System.out.println("Сайт не был найден в репозитории. Создается новый объект");
                siteEntity = new SiteEntity();
                siteEntity.setUrl(indexingSite.getUrl());
                siteEntity.setName(indexingSite.getName());
                siteEntity.setStatus(Status.INDEXING);
                siteEntity.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteEntity);
            }

            PageParser pageParser = new PageParser(
                    url,
                    siteEntity,
                    pageRepository,
                    siteRepository,
                    lemmaRepository,
                    searchIndexRepository);
            pageParser.parsePage();
            return new IndexingResponse(true);
        }
        else {
            return new IndexingResponse(false,
                    "Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        }
    }

    private void indexSite(Site site) throws ExecutionException, InterruptedException {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);

        TreeSet<String> hrefList = new TreeSet<>();
        hrefList.add(site.getUrl());

        forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        SiteParser siteParser = new SiteParser(
                site.getUrl(),
                hrefList,
                siteEntity,
                pageRepository,
                siteRepository,
                lemmaRepository,
                searchIndexRepository);
        forkJoinPool.execute(siteParser);

        forkJoinPool.shutdown();

        siteEntity.setStatus(Status.INDEXED);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);
    }
}
