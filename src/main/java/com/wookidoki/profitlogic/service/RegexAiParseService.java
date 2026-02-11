package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.dto.CalculateRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RegexAiParseService implements AiParseService {

    @Override
    public CalculateRequest parse(String text) {
        return CalculateRequest.builder()
                .price(extract(text, "판매가", "판매", "가격", "개당", "단가"))
                .variableCost(extract(text, "변동비", "재료비", "원가", "재료", "변동"))
                .fixedCost(extract(text, "고정비", "임대료", "월세", "고정", "렌트"))
                .workHours(extractInt(text, "근무시간", "근무", "작업시간", "시간"))
                .hourlyWage(extract(text, "시급", "시간당"))
                .targetProfit(extract(text, "목표이익", "목표수익", "목표 이익", "목표 수익", "이익목표"))
                .build();
    }

    private BigDecimal extract(String text, String... keywords) {
        for (String keyword : keywords) {
            BigDecimal value = findNumberAfterKeyword(text, keyword);
            if (value != null) {
                return value;
            }
        }
        return BigDecimal.ZERO;
    }

    private Integer extractInt(String text, String... keywords) {
        BigDecimal value = extract(text, keywords);
        return value.intValue();
    }

    private BigDecimal findNumberAfterKeyword(String text, String keyword) {
        String escaped = Pattern.quote(keyword);

        // 패턴: 키워드 + (선택적 구분자) + 숫자 (한글 단위 포함)
        Pattern pattern = Pattern.compile(
                escaped + "[\\s:=은는이가에서로]*([0-9,]+\\.?[0-9]*)\\s*(만|천)?\\s*원?",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return parseKoreanNumber(matcher.group(1), matcher.group(2));
        }

        // 역방향: 숫자 + 단위 + 키워드
        Pattern reversePattern = Pattern.compile(
                "([0-9,]+\\.?[0-9]*)\\s*(만|천)?\\s*원?\\s*" + escaped,
                Pattern.CASE_INSENSITIVE
        );
        Matcher reverseMatcher = reversePattern.matcher(text);
        if (reverseMatcher.find()) {
            return parseKoreanNumber(reverseMatcher.group(1), reverseMatcher.group(2));
        }

        return null;
    }

    private BigDecimal parseKoreanNumber(String numberStr, String unit) {
        if (numberStr == null || numberStr.isBlank()) {
            return BigDecimal.ZERO;
        }

        String cleaned = numberStr.replace(",", "");
        BigDecimal value = new BigDecimal(cleaned);

        if (unit != null) {
            switch (unit) {
                case "만" -> value = value.multiply(BigDecimal.valueOf(10_000));
                case "천" -> value = value.multiply(BigDecimal.valueOf(1_000));
            }
        }

        return value;
    }
}
