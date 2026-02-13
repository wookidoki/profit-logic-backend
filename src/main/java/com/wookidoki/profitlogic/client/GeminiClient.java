package com.wookidoki.profitlogic.client;

import com.wookidoki.profitlogic.client.dto.GeminiApiRequest;
import com.wookidoki.profitlogic.client.dto.GeminiApiResponse;
import com.wookidoki.profitlogic.common.exception.BusinessLogicException;
import com.wookidoki.profitlogic.config.LlmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
@Primary
public class GeminiClient implements LlmClient {

    private final LlmProperties.GeminiConfig config;
    private final WebClient webClient;

    public GeminiClient(LlmProperties llmProperties, WebClient geminiWebClient) {
        this.config = llmProperties.getGemini();
        this.webClient = geminiWebClient;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        return callApi(config.getModelLight(), config.getMaxTokensLight(),
                systemPrompt, userMessage).extractText();
    }

    @Override
    public String chatHeavy(String systemPrompt, String userMessage) {
        return callApi(config.getModelHeavy(), config.getMaxTokensHeavy(),
                systemPrompt, userMessage).extractText();
    }

    @Override
    public LlmResponse chatWithUsage(String systemPrompt, String userMessage) {
        GeminiApiResponse response = callApi(config.getModelLight(),
                config.getMaxTokensLight(), systemPrompt, userMessage);

        return LlmResponse.builder()
                .content(response.extractText())
                .inputTokens(response.getPromptTokenCount())
                .outputTokens(response.getCandidatesTokenCount())
                .totalTokens(response.getTotalTokenCount())
                .build();
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    private GeminiApiResponse callApi(String model, int maxTokens,
                                       String systemPrompt, String userMessage) {
        validateApiKey();

        GeminiApiRequest request = GeminiApiRequest.of(systemPrompt, userMessage, maxTokens);
        String uri = "/models/" + model + ":generateContent?key=" + config.getApiKey();

        try {
            GeminiApiResponse response = webClient.post()
                    .uri(uri)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiApiResponse.class)
                    .block();

            if (response == null || response.getCandidates() == null
                    || response.getCandidates().isEmpty()) {
                throw new BusinessLogicException("AI 응답이 비어있습니다.");
            }

            log.debug("Gemini API 호출 완료 - 모델: {}, 토큰: {}", model,
                    response.getTotalTokenCount());
            return response;

        } catch (WebClientResponseException e) {
            handleHttpError(e);
            throw new BusinessLogicException("AI 서비스 호출 중 오류가 발생했습니다.");
        } catch (WebClientRequestException e) {
            log.error("Gemini API 연결 실패: {}", e.getMessage());
            throw new BusinessLogicException("AI 응답 시간이 초과되었습니다.");
        } catch (Exception e) {
            log.error("Gemini API 예상치 못한 오류 - {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw new BusinessLogicException("AI 서비스 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void validateApiKey() {
        if (!isAvailable()) {
            throw new BusinessLogicException("AI 기능을 사용하려면 Gemini API 키 설정이 필요합니다.");
        }
    }

    private void handleHttpError(WebClientResponseException e) {
        HttpStatusCode status = e.getStatusCode();
        log.error("Gemini API 오류 - 상태: {}, 응답: {}", status.value(), e.getResponseBodyAsString());

        if (status.value() == 429) {
            throw new BusinessLogicException("API 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
        }
        if (status.value() == 400) {
            throw new BusinessLogicException("AI 요청 형식이 올바르지 않습니다.");
        }
        throw new BusinessLogicException("AI 서비스 호출 중 오류가 발생했습니다.");
    }
}
