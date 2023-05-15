package searchengine.services.parsing;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.Site;
import searchengine.model.entities.PageEntity;
import searchengine.model.entities.SiteEntity;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

public class SiteParser extends RecursiveAction {
    private final String url;
    private final TreeSet<String> hrefList;
    private PageEntity page;
    private SiteEntity site;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private Document document;

    public SiteParser(String url,
                      TreeSet<String> hrefList,
                      SiteEntity site,
                      PageRepository pageRepository,
                      SiteRepository siteRepository) {
        this.url = url;
        this.hrefList = hrefList;
        this.site = site;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }

    @Override
    @SneakyThrows
    protected void compute() {

        Thread.sleep(250);
        Connection connection = Jsoup.connect(url)
                .ignoreContentType(true)
                .userAgent(new UserAgent().getUserAgent())
                .referrer("https://www.google.com");
        try {
            document = connection.execute().parse();
            PageEntity page = new PageEntity();
            page.setSiteID(site);
            page.setPagePath(url);
            page.setPageContent(document.text());
            page.setPageCode(connection.response().statusCode());
            pageRepository.save(page);

            List<String> links = collectLinks(url);
            List<SiteParser> tasks = new ArrayList<>();
            for (String link : links)
            {
                if (!hrefList.contains(link) && !links.isEmpty())
                {
                    SiteParser siteParser = new SiteParser(link, hrefList, site, pageRepository, siteRepository);
                    siteParser.fork();
                    hrefList.add(link);
                    System.out.println("parsing " + link);
                    tasks.add(siteParser);
                }
            }

            for (SiteParser task : tasks)
            {
                task.join();
            }

        }
        catch (HttpStatusException e) {
            System.out.println(url + "can't be parsed");
        }
    }

    @SneakyThrows
    public synchronized List<String> collectLinks(String url) {
        List<String> linkList = new ArrayList<>();
        linkList.add(url);

        Elements links = document.select("a[href]");

        for (Element element : links) {
            String link = element.attr("abs:href");

            if (!link.startsWith(url)) {
                continue;
            }
            if (link.contains("#")) {
                continue;
            }
            if (link.endsWith(".shtml") ||
                    link.endsWith(".pdf") ||
                    link.endsWith(".xml") ||
                    link.endsWith("?main_click") ||
                    link.contains("?page=") ||
                    link.contains("?ref")) {
                continue;
            }
            if (linkList.contains(link)) {
                continue;
            }
            linkList.add(link);
        }
        return linkList;
    }
}
