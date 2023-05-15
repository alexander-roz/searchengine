package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.entities.LemmaEntity;
import searchengine.model.entities.PageEntity;
import searchengine.model.entities.SiteEntity;
import searchengine.model.entities.Status;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private boolean indexed;

    public StatisticsServiceImpl(SitesList sites,
                                 SiteRepository siteRepository,
                                 PageRepository pageRepository,
                                 LemmaRepository lemmaRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();

        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            SiteEntity siteEntity =  getSiteFromRepository(site, siteRepository);
            if(siteEntity != null && siteEntity.getStatus().equals(Status.INDEXED)) {
                int pages = countPages(siteEntity, pageRepository);
                int lemmas = countLemmas(siteEntity, lemmaRepository);
                item.setPages(pages);
                item.setLemmas(lemmas);
                item.setStatus(String.valueOf(siteEntity.getStatus()));
                item.setError(siteEntity.getLastError());
                long date = (ZonedDateTime.of(siteEntity.getStatusTime(), ZoneId.systemDefault()))
                        .toInstant().toEpochMilli();
                item.setStatusTime(date);
                total.setPages(total.getPages() + pages);
                total.setLemmas(total.getLemmas() + lemmas);
            }
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private int countPages(SiteEntity site, PageRepository pageRepository){
        int count = 0;
        List<PageEntity> pageEntityList = pageRepository.findAll();
        for(PageEntity page:pageEntityList){
            if(page.getSiteID().getSiteID() == site.getSiteID()){
                count++;
            }
        }
        return count;
    }

    private int countLemmas(SiteEntity site, LemmaRepository lemmaRepository){
        int count = 0;
        List<LemmaEntity> lemmaEntityList = lemmaRepository.findAll();
        for(LemmaEntity lemma:lemmaEntityList){
            if(lemma.getSiteID().getSiteID() == site.getSiteID()){
                count++;
            }
        }
        return count;
    }

    private SiteEntity getSiteFromRepository(Site site, SiteRepository siteRepository){
        List<SiteEntity> siteEntityList = siteRepository.findAll();
        SiteEntity resultEntity = null;
        for(SiteEntity siteEntity:siteEntityList){
            if(siteEntity.getName().equals(site.getName())){
                resultEntity = siteEntity;
            }
        }
        return resultEntity;
    }
}
