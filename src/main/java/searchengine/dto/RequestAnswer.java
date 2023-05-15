package searchengine.dto;

import lombok.Data;

@Data
public class RequestAnswer {
    private boolean result;
    private String error;

    public RequestAnswer(boolean result) {
        this.result = result;
    }

    public RequestAnswer(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
