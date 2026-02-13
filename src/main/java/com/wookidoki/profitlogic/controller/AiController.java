package com.wookidoki.profitlogic.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wookidoki.profitlogic.client.LlmClient;
import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.domain.CreatorCategory;
import com.wookidoki.profitlogic.dto.ai.AiParseRequest;
import com.wookidoki.profitlogic.dto.ai.AiParseResponse;
import com.wookidoki.profitlogic.dto.finance.CalculateRequest;
import com.wookidoki.profitlogic.service.RegexAiParseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final RegexAiParseService aiParseService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    private static final String PARSE_PROMPT = """
            사용자가 자신의 사이드 프로젝트를 설명합니다. 텍스트에서 다음 값을 추출하세요.
            값이 없으면 0으로 설정하세요.

            추출할 항목:
            - price: 건당/화당/개당 판매 수익 (원)
            - variable_cost: 건당 변동비/재료비/제작비 (원)
            - fixed_cost: 월 고정비/구독료/임대료/서버비 (원)
            - work_hours: 월 작업시간 (시간). "하루 X시간"이면 X*22로 계산
            - hourly_wage: 본업 시급 또는 목표 시급 (원). 없으면 9860 (최저시급)
            - detected_category: WEB_NOVEL, SHORT_FORM, EMOTICON, BLOG, INDIE_DEV 중 하나. 감지 불가하면 null

            반드시 아래 JSON 형식만 출력하세요. 다른 텍스트 없이 JSON만:
            {"price":0,"variable_cost":0,"fixed_cost":0,"work_hours":0,"hourly_wage":9860,"detected_category":null}
            """;

    @PostMapping("/parse")
    public ResponseEntity<ResponseData<AiParseResponse>> parse(
            @Valid @RequestBody AiParseRequest request) {

        // LLM이 가용하면 Gemini로 파싱 시도
        if (llmClient.isAvailable()) {
            try {
                String llmResult = llmClient.chat(PARSE_PROMPT, request.getText());
                log.info("LLM 파싱 응답: {}", llmResult);
                AiParseResponse response = parseLlmResponse(llmResult);
                if (response != null) {
                    return ResponseEntity.ok(ResponseData.success(response));
                }
                log.warn("LLM 파싱 결과 파싱 실패, 정규식 폴백");
            } catch (Exception e) {
                log.warn("LLM 파싱 실패, 정규식 폴백 - 원인: {}", e.getMessage());
            }
        }

        // 정규식 폴백
        CalculateRequest calcResult = aiParseService.parse(request.getText());
        CreatorCategory detected = aiParseService.detectCategory(request.getText());

        String suggestion = null;
        if (detected != null) {
            suggestion = detected.getDisplayName() + "으로 감지되었습니다. 맞춤 분석을 추천합니다.";
        }

        AiParseResponse response = AiParseResponse.from(
                calcResult, detected != null ? detected.name() : null, suggestion);

        return ResponseEntity.ok(ResponseData.success(response));
    }

    private AiParseResponse parseLlmResponse(String llmResult) {
        try {
            // JSON 블록 추출 (```json ... ``` 또는 순수 JSON)
            String json = llmResult.trim();
            if (json.contains("{")) {
                json = json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1);
            }

            JsonNode node = objectMapper.readTree(json);

            BigDecimal price = getBigDecimal(node, "price");
            BigDecimal variableCost = getBigDecimal(node, "variable_cost");
            BigDecimal fixedCost = getBigDecimal(node, "fixed_cost");
            int workHours = node.has("work_hours") ? node.get("work_hours").asInt(0) : 0;
            BigDecimal hourlyWage = getBigDecimal(node, "hourly_wage");
            if (hourlyWage.compareTo(BigDecimal.ZERO) == 0) {
                hourlyWage = BigDecimal.valueOf(9860);
            }

            String detectedCategory = node.has("detected_category") && !node.get("detected_category").isNull()
                    ? node.get("detected_category").asText() : null;

            BigDecimal targetProfit = hourlyWage.multiply(BigDecimal.valueOf(workHours));

            String suggestion = null;
            if (detectedCategory != null) {
                try {
                    CreatorCategory cat = CreatorCategory.valueOf(detectedCategory);
                    suggestion = cat.getDisplayName() + "으로 감지되었습니다. AI가 분석한 결과입니다.";
                } catch (IllegalArgumentException ignored) {
                    detectedCategory = null;
                }
            }

            return AiParseResponse.builder()
                    .price(price)
                    .variableCost(variableCost)
                    .fixedCost(fixedCost)
                    .workHours(workHours)
                    .hourlyWage(hourlyWage)
                    .targetProfit(targetProfit)
                    .detectedCategory(detectedCategory)
                    .suggestion(suggestion)
                    .build();
        } catch (Exception e) {
            log.warn("LLM JSON 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private BigDecimal getBigDecimal(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return BigDecimal.valueOf(node.get(field).asDouble(0));
        }
        return BigDecimal.ZERO;
    }
}
