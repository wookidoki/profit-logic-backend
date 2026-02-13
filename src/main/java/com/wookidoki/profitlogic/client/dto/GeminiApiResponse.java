package com.wookidoki.profitlogic.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class GeminiApiResponse {

    private List<Candidate> candidates;
    private UsageMetadata usageMetadata;

    public String extractText() {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }
        Candidate first = candidates.get(0);
        if (first.getContent() == null || first.getContent().getParts() == null
                || first.getContent().getParts().isEmpty()) {
            return "";
        }
        String text = first.getContent().getParts().get(0).getText();
        return text != null ? text : "";
    }

    public int getPromptTokenCount() {
        return usageMetadata != null ? usageMetadata.getPromptTokenCount() : 0;
    }

    public int getCandidatesTokenCount() {
        return usageMetadata != null ? usageMetadata.getCandidatesTokenCount() : 0;
    }

    public int getTotalTokenCount() {
        return usageMetadata != null ? usageMetadata.getTotalTokenCount() : 0;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class Candidate {
        private Content content;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class Content {
        private List<Part> parts;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class Part {
        private String text;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class UsageMetadata {
        private int promptTokenCount;
        private int candidatesTokenCount;
        private int totalTokenCount;
    }
}
