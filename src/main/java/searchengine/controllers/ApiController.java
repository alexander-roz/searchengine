package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.searching.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.ParsingService;
import searchengine.services.SearchingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final ParsingService parsingService;
    private final SearchingService searchingService;

    public ApiController(StatisticsService statisticsService,
                         ParsingService parsingService,
                         SearchingService searchingService
    ) {
        this.statisticsService = statisticsService;
        this.parsingService = parsingService;
        this.searchingService = searchingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        IndexingResponse requestAnswer = parsingService.startIndexing();
        return ResponseEntity.ok(requestAnswer);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(parsingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> parsePage(String url) {
        return ResponseEntity.ok(parsingService.indexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam final String query,
            @RequestParam(required = false) final String site,
            @RequestParam final Integer offset,
            @RequestParam final Integer limit) {

        return ResponseEntity.ok(searchingService.getSearchResults(query, site, offset, limit));
    }

}
