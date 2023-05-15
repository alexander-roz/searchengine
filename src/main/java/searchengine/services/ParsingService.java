package searchengine.services;

import searchengine.dto.RequestAnswer;

public interface ParsingService {
    RequestAnswer startIndexing();
    RequestAnswer stopIndexing();
}
