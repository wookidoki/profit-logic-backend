package com.wookidoki.profitlogic.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
@Getter
@Setter
public class LlmProperties {

    private String provider;
    private GeminiConfig gemini = new GeminiConfig();

    @Getter
    @Setter
    public static class GeminiConfig {
        private String apiKey;
        private String modelLight;
        private String modelHeavy;
        private int maxTokensLight;
        private int maxTokensHeavy;
        private String baseUrl;
    }
}
