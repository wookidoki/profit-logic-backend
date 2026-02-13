package com.wookidoki.profitlogic.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wookidoki.profitlogic.client.LlmClient;
import com.wookidoki.profitlogic.client.PromptTemplates;
import com.wookidoki.profitlogic.domain.CostCategory;
import com.wookidoki.profitlogic.dto.cost.CostDetailCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvCostParseService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    private static final String CSV_PARSE_PROMPT = PromptTemplates.CSV_PARSER + """

            카테고리는 반드시 다음 중 하나: API_USAGE, SERVER, TOOL_SUBSCRIPTION, MATERIAL, MARKETING, OUTSOURCING, OTHER
            비용 유형은 반드시: FIXED 또는 VARIABLE

            반드시 아래 JSON 배열 형식만 출력하세요. 다른 텍스트 없이 JSON만:
            [{"cost_name":"항목명","amount":10000,"cost_type":"FIXED","category":"OTHER","memo":""}]
            """;

    /**
     * CSV 텍스트를 파싱하여 CostDetailCreateRequest 목록으로 변환한다.
     * LLM 가용 시 Gemini로 파싱, 실패 시 수동 CSV 파싱으로 폴백.
     */
    public ParseResult parse(String csvContent) {
        // LLM 파싱 시도
        if (llmClient.isAvailable()) {
            try {
                String llmResult = llmClient.chat(CSV_PARSE_PROMPT, csvContent);
                log.info("CSV LLM 파싱 응답: {}", llmResult);
                List<CostDetailCreateRequest> items = parseLlmResponse(llmResult);
                if (items != null && !items.isEmpty()) {
                    return new ParseResult(items, List.of());
                }
                log.warn("LLM CSV 파싱 결과가 비어있음, 수동 파싱 폴백");
            } catch (Exception e) {
                log.warn("LLM CSV 파싱 실패, 수동 파싱 폴백 - 원인: {}", e.getMessage());
            }
        }

        // 수동 CSV 파싱 폴백
        return parseManually(csvContent);
    }

    private List<CostDetailCreateRequest> parseLlmResponse(String llmResult) {
        try {
            String json = llmResult.trim();
            if (json.contains("[")) {
                json = json.substring(json.indexOf("["), json.lastIndexOf("]") + 1);
            }

            JsonNode arrayNode = objectMapper.readTree(json);
            if (!arrayNode.isArray()) return null;

            List<CostDetailCreateRequest> items = new ArrayList<>();
            for (JsonNode node : arrayNode) {
                String costName = node.has("cost_name") ? node.get("cost_name").asText("") : "";
                double amount = node.has("amount") ? node.get("amount").asDouble(0) : 0;
                String costType = node.has("cost_type") ? node.get("cost_type").asText("FIXED") : "FIXED";
                String categoryStr = node.has("category") ? node.get("category").asText("OTHER") : "OTHER";
                String memo = node.has("memo") ? node.get("memo").asText("") : "";

                if (costName.isBlank() || amount <= 0) continue;

                CostCategory category;
                try {
                    category = CostCategory.valueOf(categoryStr);
                } catch (IllegalArgumentException e) {
                    category = CostCategory.OTHER;
                }

                if (!costType.equals("FIXED") && !costType.equals("VARIABLE")) {
                    costType = "FIXED";
                }

                items.add(CostDetailCreateRequest.builder()
                        .costName(costName)
                        .amount(BigDecimal.valueOf(amount))
                        .costType(costType)
                        .category(category)
                        .memo(memo.isBlank() ? null : memo)
                        .build());
            }
            return items;
        } catch (Exception e) {
            log.warn("LLM CSV JSON 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 수동 CSV 파싱: 헤더 행의 컬럼명을 매핑하여 데이터 추출
     */
    private ParseResult parseManually(String csvContent) {
        List<CostDetailCreateRequest> items = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                errors.add("CSV 파일이 비어있습니다.");
                return new ParseResult(items, errors);
            }

            String[] headers = splitCsvLine(headerLine);
            Map<String, Integer> columnMap = mapColumns(headers);

            if (!columnMap.containsKey("name") || !columnMap.containsKey("amount")) {
                errors.add("필수 컬럼(비용명, 금액)을 찾을 수 없습니다. 헤더: " + headerLine);
                return new ParseResult(items, errors);
            }

            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) continue;

                try {
                    String[] values = splitCsvLine(line);
                    CostDetailCreateRequest item = parseRow(values, columnMap, rowNum);
                    if (item != null) {
                        items.add(item);
                    }
                } catch (Exception e) {
                    errors.add(rowNum + "행: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            errors.add("CSV 파일 읽기 실패: " + e.getMessage());
        }

        return new ParseResult(items, errors);
    }

    private String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder field = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                fields.add(field.toString().trim());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString().trim());
        return fields.toArray(new String[0]);
    }

    private Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].toLowerCase().trim()
                    .replace("\"", "").replace("'", "");

            if (h.contains("비용명") || h.contains("항목명") || h.contains("항목") || h.contains("이름")
                    || h.contains("name") || h.contains("cost_name")) {
                map.put("name", i);
            } else if (h.contains("금액") || h.contains("비용") || h.contains("가격") || h.contains("원")
                    || h.contains("amount") || h.contains("price") || h.contains("cost")) {
                map.putIfAbsent("amount", i);
            } else if (h.contains("유형") || h.contains("타입") || h.contains("type") || h.contains("cost_type")) {
                map.put("type", i);
            } else if (h.contains("카테고리") || h.contains("분류") || h.contains("category")) {
                map.put("category", i);
            } else if (h.contains("메모") || h.contains("비고") || h.contains("memo") || h.contains("note")) {
                map.put("memo", i);
            }
        }
        return map;
    }

    private CostDetailCreateRequest parseRow(String[] values, Map<String, Integer> columnMap, int rowNum) {
        String costName = getField(values, columnMap, "name");
        if (costName == null || costName.isBlank()) return null;

        String amountStr = getField(values, columnMap, "amount");
        if (amountStr == null || amountStr.isBlank()) return null;

        BigDecimal amount;
        try {
            String cleaned = amountStr.replace(",", "").replace("원", "").replace("₩", "").trim();
            amount = new BigDecimal(cleaned);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) return null;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("금액 파싱 실패: " + amountStr);
        }

        String typeStr = getField(values, columnMap, "type");
        String costType = parseCostType(typeStr);

        String categoryStr = getField(values, columnMap, "category");
        CostCategory category = parseCostCategory(categoryStr);

        String memo = getField(values, columnMap, "memo");

        return CostDetailCreateRequest.builder()
                .costName(costName)
                .amount(amount)
                .costType(costType)
                .category(category)
                .memo(memo == null || memo.isBlank() ? null : memo)
                .build();
    }

    private String getField(String[] values, Map<String, Integer> columnMap, String key) {
        Integer idx = columnMap.get(key);
        if (idx == null || idx >= values.length) return null;
        return values[idx].replace("\"", "").trim();
    }

    private String parseCostType(String typeStr) {
        if (typeStr == null) return "FIXED";
        String lower = typeStr.toLowerCase().trim();
        if (lower.contains("variable") || lower.contains("변동") || lower.contains("건당")) {
            return "VARIABLE";
        }
        return "FIXED";
    }

    private CostCategory parseCostCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.isBlank()) return CostCategory.OTHER;
        String upper = categoryStr.toUpperCase().trim().replace(" ", "_");
        try {
            return CostCategory.valueOf(upper);
        } catch (IllegalArgumentException e) {
            // 한글 카테고리 매핑
            String lower = categoryStr.toLowerCase();
            if (lower.contains("api") || lower.contains("에이피아이")) return CostCategory.API_USAGE;
            if (lower.contains("서버") || lower.contains("호스팅")) return CostCategory.SERVER;
            if (lower.contains("구독") || lower.contains("tool") || lower.contains("도구")) return CostCategory.TOOL_SUBSCRIPTION;
            if (lower.contains("재료") || lower.contains("원자재") || lower.contains("material")) return CostCategory.MATERIAL;
            if (lower.contains("마케팅") || lower.contains("광고") || lower.contains("홍보")) return CostCategory.MARKETING;
            if (lower.contains("외주") || lower.contains("인건비") || lower.contains("outsourc")) return CostCategory.OUTSOURCING;
            return CostCategory.OTHER;
        }
    }

    public record ParseResult(List<CostDetailCreateRequest> items, List<String> errors) {}
}
