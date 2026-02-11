package com.wookidoki.profitlogic.client.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeminiApiResponse 역직렬화 테스트")
class GeminiApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("정상 응답 파싱")
    class ValidResponse {

        @Test
        @DisplayName("Gemini API 응답 JSON → 텍스트 추출")
        void shouldExtractText() throws Exception {
            String json = """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              {"text": "손익분기점은 63개입니다."}
                            ]
                          }
                        }
                      ],
                      "usageMetadata": {
                        "promptTokenCount": 150,
                        "candidatesTokenCount": 30,
                        "totalTokenCount": 180
                      }
                    }
                    """;

            GeminiApiResponse response = objectMapper.readValue(json, GeminiApiResponse.class);

            assertThat(response.extractText()).isEqualTo("손익분기점은 63개입니다.");
            assertThat(response.getPromptTokenCount()).isEqualTo(150);
            assertThat(response.getCandidatesTokenCount()).isEqualTo(30);
            assertThat(response.getTotalTokenCount()).isEqualTo(180);
        }

        @Test
        @DisplayName("usageMetadata 없는 응답 → 토큰 0 반환")
        void shouldHandleMissingUsageMetadata() throws Exception {
            String json = """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [{"text": "응답입니다"}]
                          }
                        }
                      ]
                    }
                    """;

            GeminiApiResponse response = objectMapper.readValue(json, GeminiApiResponse.class);

            assertThat(response.extractText()).isEqualTo("응답입니다");
            assertThat(response.getPromptTokenCount()).isZero();
            assertThat(response.getCandidatesTokenCount()).isZero();
            assertThat(response.getTotalTokenCount()).isZero();
        }
    }

    @Nested
    @DisplayName("비정상 응답 처리")
    class InvalidResponse {

        @Test
        @DisplayName("candidates 없는 응답 → 빈 문자열")
        void shouldReturnEmptyForNoCandidates() throws Exception {
            String json = """
                    {
                      "candidates": [],
                      "usageMetadata": {
                        "promptTokenCount": 10,
                        "candidatesTokenCount": 0,
                        "totalTokenCount": 10
                      }
                    }
                    """;

            GeminiApiResponse response = objectMapper.readValue(json, GeminiApiResponse.class);
            assertThat(response.extractText()).isEmpty();
        }

        @Test
        @DisplayName("content가 null인 응답 → 빈 문자열")
        void shouldReturnEmptyForNullContent() throws Exception {
            String json = """
                    {
                      "candidates": [
                        {"content": null}
                      ]
                    }
                    """;

            GeminiApiResponse response = objectMapper.readValue(json, GeminiApiResponse.class);
            assertThat(response.extractText()).isEmpty();
        }

        @Test
        @DisplayName("parts 빈 배열 → 빈 문자열")
        void shouldReturnEmptyForEmptyParts() throws Exception {
            String json = """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": []
                          }
                        }
                      ]
                    }
                    """;

            GeminiApiResponse response = objectMapper.readValue(json, GeminiApiResponse.class);
            assertThat(response.extractText()).isEmpty();
        }
    }
}
