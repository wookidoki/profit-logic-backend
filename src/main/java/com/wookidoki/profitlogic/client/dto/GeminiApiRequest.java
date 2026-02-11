package com.wookidoki.profitlogic.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class GeminiApiRequest {

    private SystemInstruction systemInstruction;
    private List<Content> contents;
    private GenerationConfig generationConfig;

    public static GeminiApiRequest of(String systemPrompt, String userMessage, int maxTokens) {
        return GeminiApiRequest.builder()
                .systemInstruction(new SystemInstruction(List.of(new Part(systemPrompt))))
                .contents(List.of(new Content("user", List.of(new Part(userMessage)))))
                .generationConfig(new GenerationConfig(maxTokens, 0.7))
                .build();
    }

    @Getter
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class SystemInstruction {
        private List<Part> parts;
    }

    @Getter
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class Content {
        private String role;
        private List<Part> parts;
    }

    @Getter
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class Part {
        private String text;
    }

    @Getter
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class GenerationConfig {
        private int maxOutputTokens;
        private double temperature;
    }
}
