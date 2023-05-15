package searchengine.controllers;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.Application;
import searchengine.dto.RequestAnswer;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import searchengine.services.ParsingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final ParsingService parsingService;

    public ApiController(StatisticsService statisticsService,
                         ParsingService parsingService) {
        this.statisticsService = statisticsService;
        this.parsingService = parsingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<RequestAnswer> startIndexing() {
        RequestAnswer requestAnswer = parsingService.startIndexing();
        return ResponseEntity.ok(requestAnswer);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<RequestAnswer> stopIndexing() {
        return ResponseEntity.ok(parsingService.stopIndexing());
    }

}
