package com.wookidoki.profitlogic.client;

import com.wookidoki.profitlogic.client.dto.GeminiApiResponse;
import com.wookidoki.profitlogic.common.exception.BusinessLogicException;
import com.wookidoki.profitlogic.config.LlmProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeminiClient 단위 테스트")
class GeminiClientTest {

    @Mock
    private WebClient webClient;

    private LlmProperties llmProperties;
    private GeminiClient geminiClient;

    @BeforeEach
    void setUp() {
        llmProperties = new LlmProperties();
        LlmProperties.GeminiConfig geminiConfig = new LlmProperties.GeminiConfig();
        geminiConfig.setApiKey("");
        geminiConfig.setModelLight("gemini-2.5-flash");
        geminiConfig.setModelHeavy("gemini-2.5-pro");
        geminiConfig.setMaxTokensLight(1000);
        geminiConfig.setMaxTokensHeavy(4000);
        geminiConfig.setBaseUrl("https://generativelanguage.googleapis.com/v1beta");
        llmProperties.setProvider("gemini");
        llmProperties.setGemini(geminiConfig);

        geminiClient = new GeminiClient(llmProperties, webClient);
    }

    @Nested
    @DisplayName("isAvailable")
    class IsAvailable {

        @Test
        @DisplayName("API 키 빈 문자열 → false")
        void shouldReturnFalseWhenApiKeyEmpty() {
            assertThat(geminiClient.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("API 키 null → false")
        void shouldReturnFalseWhenApiKeyNull() {
            llmProperties.getGemini().setApiKey(null);
            GeminiClient client = new GeminiClient(llmProperties, webClient);
            assertThat(client.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("API 키 설정됨 → true")
        void shouldReturnTrueWhenApiKeySet() {
            llmProperties.getGemini().setApiKey("test-api-key");
            GeminiClient client = new GeminiClient(llmProperties, webClient);
            assertThat(client.isAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("API 키 미설정 시 호출")
    class NoApiKey {

        @Test
        @DisplayName("chat() → BusinessLogicException")
        void chatShouldThrowWhenNoApiKey() {
            assertThatThrownBy(() -> geminiClient.chat("system", "user"))
                    .isInstanceOf(BusinessLogicException.class)
                    .hasMessageContaining("API 키 설정이 필요합니다");
        }

        @Test
        @DisplayName("chatHeavy() → BusinessLogicException")
        void chatHeavyShouldThrowWhenNoApiKey() {
            assertThatThrownBy(() -> geminiClient.chatHeavy("system", "user"))
                    .isInstanceOf(BusinessLogicException.class)
                    .hasMessageContaining("API 키 설정이 필요합니다");
        }

        @Test
        @DisplayName("chatWithUsage() → BusinessLogicException")
        void chatWithUsageShouldThrowWhenNoApiKey() {
            assertThatThrownBy(() -> geminiClient.chatWithUsage("system", "user"))
                    .isInstanceOf(BusinessLogicException.class)
                    .hasMessageContaining("API 키 설정이 필요합니다");
        }
    }

    @Nested
    @DisplayName("정상 API 호출")
    class SuccessfulCall {

        @BeforeEach
        void setUpApiKey() {
            llmProperties.getGemini().setApiKey("test-api-key");
            geminiClient = new GeminiClient(llmProperties, webClient);
        }

        @Test
        @DisplayName("chat() → 응답 텍스트 반환")
        @SuppressWarnings("unchecked")
        void chatShouldReturnText() {
            GeminiApiResponse mockResponse = createMockResponse("분석 결과입니다.", 100, 20, 120);
            mockWebClientCall(mockResponse);

            String result = geminiClient.chat("system prompt", "user message");
            assertThat(result).isEqualTo("분석 결과입니다.");
        }

        @Test
        @DisplayName("chatWithUsage() → LlmResponse 반환")
        @SuppressWarnings("unchecked")
        void chatWithUsageShouldReturnLlmResponse() {
            GeminiApiResponse mockResponse = createMockResponse("결과", 150, 30, 180);
            mockWebClientCall(mockResponse);

            LlmResponse result = geminiClient.chatWithUsage("system", "user");

            assertThat(result.getContent()).isEqualTo("결과");
            assertThat(result.getInputTokens()).isEqualTo(150);
            assertThat(result.getOutputTokens()).isEqualTo(30);
            assertThat(result.getTotalTokens()).isEqualTo(180);
        }

        @SuppressWarnings("unchecked")
        private void mockWebClientCall(GeminiApiResponse response) {
            WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
            WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            given(webClient.post()).willReturn(requestBodyUriSpec);
            given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
            given(requestBodySpec.bodyValue(any())).willReturn(mock(WebClient.RequestHeadersSpec.class));

            WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
            given(requestBodySpec.bodyValue(any())).willReturn(headersSpec);
            given(headersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.bodyToMono(GeminiApiResponse.class)).willReturn(Mono.just(response));
        }

        private GeminiApiResponse createMockResponse(String text, int prompt, int candidates, int total) {
            GeminiApiResponse.Part part = new GeminiApiResponse.Part(text);
            GeminiApiResponse.Content content = new GeminiApiResponse.Content(List.of(part));
            GeminiApiResponse.Candidate candidate = new GeminiApiResponse.Candidate(content);
            GeminiApiResponse.UsageMetadata usage = new GeminiApiResponse.UsageMetadata(prompt, candidates, total);
            return new GeminiApiResponse(List.of(candidate), usage);
        }
    }
}
