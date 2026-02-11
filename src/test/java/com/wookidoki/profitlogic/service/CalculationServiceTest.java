package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.domain.logic.FinancialCalculator;
import com.wookidoki.profitlogic.dto.CalculateRequest;
import com.wookidoki.profitlogic.dto.CalculateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalculationService 단위 테스트")
class CalculationServiceTest {

    @Spy
    private FinancialCalculator calculator;

    @InjectMocks
    private CalculationService calculationService;

    private CalculateRequest buildRequest(String price, String variableCost, String fixedCost,
                                           int workHours, String hourlyWage, String targetProfit) {
        return CalculateRequest.builder()
                .price(new BigDecimal(price))
                .variableCost(new BigDecimal(variableCost))
                .fixedCost(new BigDecimal(fixedCost))
                .workHours(workHours)
                .hourlyWage(new BigDecimal(hourlyWage))
                .targetProfit(new BigDecimal(targetProfit))
                .build();
    }

    @Nested
    @DisplayName("Type A: 시간 투자형 (웹소설 작가)")
    class TypeACreator {

        @Test
        @DisplayName("BEP = 18, 목표판매량 = 706.90, 경제적이윤 양수 → isViable = true")
        void calculate_typeA_success() {
            CalculateRequest request = buildRequest("3000", "100", "50000", 160, "9860", "2000000");

            CalculateResponse result = calculationService.calculate(request);

            assertThat(result.getBreakEvenPoint()).isEqualByComparingTo("18");
            assertThat(result.getTargetQuantity()).isEqualByComparingTo("706.90");
            assertThat(result.getIsViable()).isTrue();
            assertThat(result.getZone()).isEqualTo("GREEN");
            assertThat(result.getAppliedHourlyWage()).isEqualByComparingTo("9860");
        }
    }

    @Nested
    @DisplayName("Type B: 비용 지출형 (쇼핑몰 셀러)")
    class TypeBSeller {

        @Test
        @DisplayName("BEP = 116, 목표판매량 = 500.00, isViable = true")
        void calculate_typeB_success() {
            CalculateRequest request = buildRequest("25000", "12000", "1500000", 200, "9860", "5000000");

            CalculateResponse result = calculationService.calculate(request);

            assertThat(result.getBreakEvenPoint()).isEqualByComparingTo("116");
            assertThat(result.getTargetQuantity()).isEqualByComparingTo("500.00");
            assertThat(result.getIsViable()).isTrue();
            assertThat(result.getZone()).isEqualTo("GREEN");
        }
    }

    @Nested
    @DisplayName("최저임금 하한 적용")
    class MinimumWageEnforcement {

        @Test
        @DisplayName("시급 5000원 입력 → 9860원으로 강제 적용")
        void calculate_belowMinimumWage_appliesMinimum() {
            CalculateRequest request = buildRequest("3000", "100", "50000", 160, "5000", "2000000");

            CalculateResponse result = calculationService.calculate(request);

            assertThat(result.getAppliedHourlyWage()).isEqualByComparingTo("9860");
        }

        @Test
        @DisplayName("시급 15000원 입력 → 그대로 유지")
        void calculate_aboveMinimumWage_keepsOriginal() {
            CalculateRequest request = buildRequest("3000", "100", "50000", 160, "15000", "2000000");

            CalculateResponse result = calculationService.calculate(request);

            assertThat(result.getAppliedHourlyWage()).isEqualByComparingTo("15000");
        }
    }

    @Nested
    @DisplayName("Zone 분류")
    class ZoneClassification {

        @Test
        @DisplayName("안전마진율 > 20% → GREEN")
        void calculate_highMargin_green() {
            CalculateRequest request = buildRequest("25000", "12000", "1500000", 200, "9860", "5000000");

            CalculateResponse result = calculationService.calculate(request);

            assertThat(result.getZone()).isEqualTo("GREEN");
        }

        @Test
        @DisplayName("안전마진율 0~20% → YELLOW")
        void calculate_lowMargin_yellow() {
            // 고정비가 매우 높아 BEP와 목표판매량이 비슷한 케이스
            CalculateRequest request = buildRequest("10000", "5000", "450000", 100, "9860", "50000");

            CalculateResponse result = calculationService.calculate(request);

            assertThat(result.getMarginRate()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(result.getMarginRate()).isLessThanOrEqualTo(new BigDecimal("20"));
            assertThat(result.getZone()).isEqualTo("YELLOW");
        }
    }

    @Nested
    @DisplayName("에러 케이스")
    class ErrorCases {

        @Test
        @DisplayName("판매가 == 변동비 → BusinessLogicException")
        void calculate_zeroContribution_throws() {
            CalculateRequest request = buildRequest("1000", "1000", "50000", 160, "9860", "100000");

            assertThatThrownBy(() -> calculationService.calculate(request))
                    .hasMessageContaining("팔수록 손해");
        }

        @Test
        @DisplayName("판매가 < 변동비 → BusinessLogicException")
        void calculate_negativeContribution_throws() {
            CalculateRequest request = buildRequest("500", "1000", "50000", 160, "9860", "100000");

            assertThatThrownBy(() -> calculationService.calculate(request))
                    .hasMessageContaining("팔수록 손해");
        }
    }

    @Nested
    @DisplayName("응답 필드 검증")
    class ResponseFields {

        @Test
        @DisplayName("모든 응답 필드가 null이 아님")
        void calculate_allFieldsPresent() {
            CalculateRequest request = buildRequest("3000", "100", "50000", 160, "9860", "2000000");

            CalculateResponse result = calculationService.calculate(request);

            assertThat(result.getBreakEvenPoint()).isNotNull();
            assertThat(result.getOperatingProfit()).isNotNull();
            assertThat(result.getEconomicProfit()).isNotNull();
            assertThat(result.getTargetQuantity()).isNotNull();
            assertThat(result.getMarginRate()).isNotNull();
            assertThat(result.getContributionMargin()).isNotNull();
            assertThat(result.getShadowWage()).isNotNull();
            assertThat(result.getIsViable()).isNotNull();
            assertThat(result.getZone()).isNotNull();
            assertThat(result.getAppliedHourlyWage()).isNotNull();
        }

        @Test
        @DisplayName("공헌이익 = 판매가 - 변동비")
        void calculate_contributionMargin_correct() {
            CalculateRequest request = buildRequest("3000", "100", "50000", 160, "9860", "2000000");

            CalculateResponse result = calculationService.calculate(request);

            assertThat(result.getContributionMargin()).isEqualByComparingTo("2900");
        }
    }
}
