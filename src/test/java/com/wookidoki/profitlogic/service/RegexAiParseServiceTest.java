package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.dto.finance.CalculateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RegexAiParseService 테스트")
class RegexAiParseServiceTest {

    private RegexAiParseService service;

    @BeforeEach
    void setUp() {
        service = new RegexAiParseService();
    }

    @Nested
    @DisplayName("기본 숫자 추출")
    class BasicExtraction {

        @Test
        @DisplayName("키워드 뒤의 숫자를 정상적으로 추출한다")
        void extractBasicNumbers() {
            String text = "판매가 15000원, 변동비 5000원, 고정비 500000원, 근무시간 160시간, 시급 9860원, 목표이익 2000000원";
            CalculateRequest result = service.parse(text);

            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(result.getVariableCost()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(result.getFixedCost()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(result.getWorkHours()).isEqualTo(160);
            assertThat(result.getHourlyWage()).isEqualByComparingTo(new BigDecimal("9860"));
            assertThat(result.getTargetProfit()).isEqualByComparingTo(new BigDecimal("2000000"));
        }

        @Test
        @DisplayName("콤마가 포함된 숫자를 처리한다")
        void extractNumbersWithCommas() {
            String text = "판매가 15,000원, 변동비 5,000원, 고정비 500,000원";
            CalculateRequest result = service.parse(text);

            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(result.getVariableCost()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(result.getFixedCost()).isEqualByComparingTo(new BigDecimal("500000"));
        }
    }

    @Nested
    @DisplayName("한글 단위 처리")
    class KoreanUnitExtraction {

        @Test
        @DisplayName("만 단위를 정상적으로 변환한다")
        void extractManUnit() {
            String text = "고정비 50만원, 목표이익 200만원";
            CalculateRequest result = service.parse(text);

            assertThat(result.getFixedCost()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(result.getTargetProfit()).isEqualByComparingTo(new BigDecimal("2000000"));
        }

        @Test
        @DisplayName("천 단위를 정상적으로 변환한다")
        void extractCheonUnit() {
            String text = "판매가 1.5만원, 변동비 5천원";
            CalculateRequest result = service.parse(text);

            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("15000.0"));
            assertThat(result.getVariableCost()).isEqualByComparingTo(new BigDecimal("5000"));
        }
    }

    @Nested
    @DisplayName("대체 키워드 처리")
    class AlternativeKeywords {

        @Test
        @DisplayName("대체 키워드로 추출한다 (재료비, 임대료 등)")
        void extractWithAltKeywords() {
            String text = "개당 가격은 15000원이고, 재료비는 5000원, 임대료 50만원";
            CalculateRequest result = service.parse(text);

            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(result.getVariableCost()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(result.getFixedCost()).isEqualByComparingTo(new BigDecimal("500000"));
        }
    }

    @Nested
    @DisplayName("빈 값 처리")
    class EmptyValues {

        @Test
        @DisplayName("키워드가 없으면 0을 반환한다")
        void returnZeroForMissingKeywords() {
            String text = "아무런 관련 없는 텍스트입니다.";
            CalculateRequest result = service.parse(text);

            assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getVariableCost()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getFixedCost()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getWorkHours()).isEqualTo(0);
            assertThat(result.getHourlyWage()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTargetProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("자연어 문장 처리")
    class NaturalLanguage {

        @Test
        @DisplayName("자연어 문장에서 값을 추출한다")
        void extractFromNaturalLanguage() {
            String text = "판매가 15,000원에 판매합니다. "
                    + "재료비 5,000원이고, 고정비 50만원입니다. "
                    + "근무시간 160시간이고, 시급 9,860원 기준으로 "
                    + "목표이익 200만원을 달성하고 싶습니다.";
            CalculateRequest result = service.parse(text);

            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(result.getVariableCost()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(result.getFixedCost()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(result.getWorkHours()).isEqualTo(160);
            assertThat(result.getHourlyWage()).isEqualByComparingTo(new BigDecimal("9860"));
            assertThat(result.getTargetProfit()).isEqualByComparingTo(new BigDecimal("2000000"));
        }
    }
}
