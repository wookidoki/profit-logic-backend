package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.domain.CreatorCategory;
import com.wookidoki.profitlogic.dto.finance.CalculateRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RegexAiParseService implements AiParseService {

    /**
     * 자연어 텍스트에서 크리에이터 카테고리를 감지한다.
     */
    public CreatorCategory detectCategory(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();

        if (lower.contains("웹소설") || lower.contains("소설") || lower.contains("연재")
                || lower.contains("작가") || lower.contains("웹툰") || lower.contains("문피아")
                || lower.contains("카카오페이지") || lower.contains("노벨피아")) {
            return CreatorCategory.WEB_NOVEL;
        }
        if (lower.contains("숏폼") || lower.contains("유튜브") || lower.contains("영상")
                || lower.contains("릴스") || lower.contains("틱톡") || lower.contains("쇼츠")
                || lower.contains("크리에이터") || lower.contains("구독자")) {
            return CreatorCategory.SHORT_FORM;
        }
        if (lower.contains("이모티콘") || lower.contains("스티커") || lower.contains("카카오")
                || lower.contains("라인")) {
            return CreatorCategory.EMOTICON;
        }
        if (lower.contains("블로그") || lower.contains("뉴스레터") || lower.contains("애드센스")
                || lower.contains("티스토리") || lower.contains("네이버 블로그") || lower.contains("포스팅")) {
            return CreatorCategory.BLOG;
        }
        if (lower.contains("앱") || lower.contains("saas") || lower.contains("개발")
                || lower.contains("서비스") || lower.contains("플러그인") || lower.contains("템플릿")
                || lower.contains("구독형")) {
            return CreatorCategory.INDIE_DEV;
        }
        return null;
    }

    @Override
    public CalculateRequest parse(String text) {
        BigDecimal price = extract(text,
                "판매가", "판매", "가격", "개당", "단가", "화당", "건당", "세트당",
                "구독료", "구독", "월정액", "수익", "매출");
        BigDecimal variableCost = extract(text,
                "변동비", "재료비", "원가", "재료", "변동", "직접비", "제작비",
                "외주비", "소모품", "수수료");
        BigDecimal fixedCost = extract(text,
                "고정비", "임대료", "월세", "고정", "렌트", "구독료비", "서버비",
                "호스팅", "유지비", "월비용", "월 비용", "관리비");
        // "월 X만원 쓰고있어" 등 지출 패턴 감지
        if (fixedCost.compareTo(BigDecimal.ZERO) == 0) {
            fixedCost = extractMonthlySpend(text);
        }
        BigDecimal hourlyWage = extract(text,
                "시급", "시간당", "최저시급", "목표시급", "기대시급", "희망시급");

        // 작업 시간: "하루 X시간" → X*22, 그냥 "XX시간" → 그대로
        Integer workHours = extractWorkHours(text);

        // 목표이익 추출 or 시급*시간으로 자동 산출
        BigDecimal targetProfit = extract(text,
                "목표이익", "목표수익", "목표 이익", "목표 수익", "이익목표",
                "목표매출", "목표 매출", "월수입", "월 수입");
        if (targetProfit.compareTo(BigDecimal.ZERO) == 0
                && hourlyWage.compareTo(BigDecimal.ZERO) > 0
                && workHours > 0) {
            targetProfit = hourlyWage.multiply(BigDecimal.valueOf(workHours));
        }

        return CalculateRequest.builder()
                .price(price)
                .variableCost(variableCost)
                .fixedCost(fixedCost)
                .workHours(workHours)
                .hourlyWage(hourlyWage)
                .targetProfit(targetProfit)
                .build();
    }

    private BigDecimal extract(String text, String... keywords) {
        for (String keyword : keywords) {
            BigDecimal value = findNumberAfterKeyword(text, keyword);
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                return value;
            }
        }
        return BigDecimal.ZERO;
    }

    private Integer extractWorkHours(String text) {
        // "하루 X시간" / "하루에 X시간" 패턴 → 월간으로 변환 (×22)
        Pattern dailyPattern = Pattern.compile(
                "하루[에서]?\\s*(?:평균\\s*)?([0-9]+\\.?[0-9]*)\\s*시간",
                Pattern.CASE_INSENSITIVE);
        Matcher dailyMatcher = dailyPattern.matcher(text);
        if (dailyMatcher.find()) {
            BigDecimal daily = new BigDecimal(dailyMatcher.group(1));
            return daily.multiply(BigDecimal.valueOf(22))
                    .setScale(0, RoundingMode.HALF_UP).intValue();
        }

        // "월 XX시간" 패턴
        Pattern monthlyPattern = Pattern.compile(
                "(?:월|한달)[에서]?\\s*(?:평균\\s*)?([0-9]+\\.?[0-9]*)\\s*시간",
                Pattern.CASE_INSENSITIVE);
        Matcher monthlyMatcher = monthlyPattern.matcher(text);
        if (monthlyMatcher.find()) {
            return new BigDecimal(monthlyMatcher.group(1))
                    .setScale(0, RoundingMode.HALF_UP).intValue();
        }

        // 일반 키워드 기반
        BigDecimal value = extract(text, "근무시간", "작업시간", "근무", "작업");
        return value.intValue();
    }

    /**
     * "월 3만원씩 쓰고있어", "매달 5만원 지출" 등 월간 지출 패턴 감지
     */
    private BigDecimal extractMonthlySpend(String text) {
        Pattern pattern = Pattern.compile(
                "(?:월|매달|한달)[에서]?\\s*([0-9,]+\\.?[0-9]*)\\s*(만|천)?\\s*원?\\s*(?:씩|정도|쯤)?\\s*(?:쓰|지출|나가|내|들|비용)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return parseKoreanNumber(matcher.group(1), matcher.group(2));
        }
        return BigDecimal.ZERO;
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
