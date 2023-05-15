package searchengine;

import searchengine.config.Site;
import searchengine.model.entities.PageEntity;
import searchengine.services.parsing.SiteParser;
import searchengine.services.parsing.UserAgent;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;

public class Main {

//    public static void main(String[] args) {
//        Site site = new Site();
//        site.setName("Et Cetera");
//        site.setUrl("https://et-cetera.ru/mobile/");
//
//        ArrayList<PageEntity> pages = new ArrayList<>();
//        TreeSet<String> hrefList = new TreeSet<>();
//        hrefList.add(site.getUrl());
//        ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
//        SiteParser siteParser = new SiteParser(site.getUrl(),hrefList);
//        pages.addAll(forkJoinPool.invoke(siteParser));
//
//        for(PageEntity page:pages){
//            System.out.println(page.getPagePath());
//        }
//
//    }
}
