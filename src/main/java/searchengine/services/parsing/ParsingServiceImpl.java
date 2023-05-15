package searchengine.services.parsing;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.RequestAnswer;
import searchengine.model.entities.SiteEntity;
import searchengine.model.entities.Status;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import searchengine.services.ParsingService;

import java.time.LocalDateTime;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;

@Service
public class ParsingServiceImpl implements ParsingService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private boolean started;
    private ForkJoinPool forkJoinPool;

    public ParsingServiceImpl(SitesList sites,
                              SiteRepository siteRepository,
                              PageRepository pageRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    @Override
    @SneakyThrows
    public RequestAnswer startIndexing() {
        if (started) {
            return new RequestAnswer(false, "Already started");
        } else {
            started = true;
        }

        pageRepository.deleteAll();
        siteRepository.deleteAll();


        for (Site site : sites.getSites()) {
            SiteEntity siteEntity = new SiteEntity();

            siteEntity.setName(site.getName());
            siteEntity.setUrl(site.getUrl());
            siteEntity.setStatus(Status.INDEXING);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);

            TreeSet<String> hrefList = new TreeSet<>();
            hrefList.add(site.getUrl());

            forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
            SiteParser siteParser = new SiteParser(site.getUrl(),hrefList,siteEntity,pageRepository,siteRepository);
            forkJoinPool.execute(siteParser);

            forkJoinPool.shutdown();
            siteEntity.setStatus(Status.INDEXED);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);

        }
        started = false;
        return new RequestAnswer(true);
    }

    @Override
    public RequestAnswer stopIndexing() {
        if (!started) {
            return new RequestAnswer(false, "Indexing is not started");
        } else {
            started = false;
        }
        forkJoinPool.shutdownNow();
        Iterable<SiteEntity> siteList = siteRepository.findAll();
        for (SiteEntity site : siteList) {
            if (site.getStatus() == Status.INDEXING) {
                site.setStatus(Status.FAILED);
                site.setStatusTime(LocalDateTime.now());
                site.setLastError("Indexing stopped");
                siteRepository.save(site);
            }
        }
        return new RequestAnswer(true);
    }
}
