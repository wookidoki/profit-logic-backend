package com.wookidoki.profitlogic.domain.logic;

import com.wookidoki.profitlogic.common.exception.BusinessLogicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FinancialCalculatorTest {

    private FinancialCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new FinancialCalculator();
    }

    @Nested
    @DisplayName("calculateBEP - 손익분기점")
    class CalculateBEPTest {

        @Test
        @DisplayName("정상: 고정비 100만, 판매가 1만, 변동비 5천 → BEP 200개")
        void shouldReturnCorrectBEP() {
            BigDecimal result = calculator.calculateBEP(
                    new BigDecimal("1000000"),
                    new BigDecimal("10000"),
                    new BigDecimal("5000")
            );
            assertEquals(new BigDecimal("200"), result);
        }

        @Test
        @DisplayName("올림 검증: 나누어 떨어지지 않으면 CEILING 올림")
        void shouldCeilWhenNotDivisible() {
            BigDecimal result = calculator.calculateBEP(
                    new BigDecimal("100000"),
                    new BigDecimal("10000"),
                    new BigDecimal("3000")
            );
            assertEquals(new BigDecimal("15"), result);
        }

        @Test
        @DisplayName("예외: 판매가 = 변동비 → BusinessLogicException")
        void shouldThrowWhenMarginZero() {
            BusinessLogicException ex = assertThrows(BusinessLogicException.class, () ->
                    calculator.calculateBEP(
                            new BigDecimal("1000000"),
                            new BigDecimal("10000"),
                            new BigDecimal("10000")
                    )
            );
            assertTrue(ex.getMessage().contains("팔수록 손해입니다"));
        }

        @Test
        @DisplayName("예외: 판매가 < 변동비 → BusinessLogicException")
        void shouldThrowWhenMarginNegative() {
            assertThrows(BusinessLogicException.class, () ->
                    calculator.calculateBEP(
                            new BigDecimal("1000000"),
                            new BigDecimal("5000"),
                            new BigDecimal("10000")
                    )
            );
        }
    }

    @Nested
    @DisplayName("calculateEconomicProfit - 경제적 이윤")
    class CalculateEconomicProfitTest {

        @Test
        @DisplayName("정상: 영업이익 50만, 10시간, 시급 1만 → 경제적 이윤 40만")
        void shouldReturnCorrectEconomicProfit() {
            BigDecimal result = calculator.calculateEconomicProfit(
                    new BigDecimal("500000"),
                    new BigDecimal("10"),
                    new BigDecimal("10000")
            );
            assertEquals(new BigDecimal("400000.00"), result);
        }

        @Test
        @DisplayName("음수: 기회비용이 영업이익보다 크면 음수 반환")
        void shouldReturnNegativeWhenOpportunityCostExceeds() {
            BigDecimal result = calculator.calculateEconomicProfit(
                    new BigDecimal("100000"),
                    new BigDecimal("20"),
                    new BigDecimal("10000")
            );
            assertEquals(new BigDecimal("-100000.00"), result);
        }
    }

    @Nested
    @DisplayName("calculateShadowWage - 실질 시급")
    class CalculateShadowWageTest {

        @Test
        @DisplayName("정상: 영업이익 100만, 160시간 → 시급 6250원")
        void shouldReturnCorrectShadowWage() {
            BigDecimal result = calculator.calculateShadowWage(
                    new BigDecimal("1000000"),
                    new BigDecimal("160")
            );
            assertEquals(new BigDecimal("6250.00"), result);
        }

        @Test
        @DisplayName("음수 영업이익 → 음수 시급 반환")
        void shouldReturnNegativeWhenLoss() {
            BigDecimal result = calculator.calculateShadowWage(
                    new BigDecimal("-500000"),
                    new BigDecimal("160")
            );
            assertEquals(new BigDecimal("-3125.00"), result);
        }

        @Test
        @DisplayName("예외: 근무시간 0 → BusinessLogicException")
        void shouldThrowWhenWorkHoursZero() {
            assertThrows(BusinessLogicException.class, () ->
                    calculator.calculateShadowWage(
                            new BigDecimal("1000000"),
                            BigDecimal.ZERO
                    )
            );
        }
    }

    @Nested
    @DisplayName("calculateTargetSales - 목표 판매량")
    class CalculateTargetSalesTest {

        @Test
        @DisplayName("정상: 고정비 100만, 목표이익 100만, 판매가 1만, 변동비 5천 → 400개")
        void shouldReturnCorrectTargetSales() {
            BigDecimal result = calculator.calculateTargetSales(
                    new BigDecimal("1000000"),
                    new BigDecimal("1000000"),
                    new BigDecimal("10000"),
                    new BigDecimal("5000")
            );
            assertEquals(new BigDecimal("400.00"), result);
        }

        @Test
        @DisplayName("예외: 공헌이익 0 이하 → BusinessLogicException")
        void shouldThrowWhenContributionNonPositive() {
            assertThrows(BusinessLogicException.class, () ->
                    calculator.calculateTargetSales(
                            new BigDecimal("1000000"),
                            new BigDecimal("1000000"),
                            new BigDecimal("5000"),
                            new BigDecimal("10000")
                    )
            );
        }
    }

    @Nested
    @DisplayName("calculateOperatingProfit - 영업이익")
    class CalculateOperatingProfitTest {

        @Test
        @DisplayName("정상: (1만 - 5천) * 200 - 100만 = 0")
        void shouldReturnCorrectOperatingProfit() {
            BigDecimal result = calculator.calculateOperatingProfit(
                    new BigDecimal("10000"),
                    new BigDecimal("5000"),
                    new BigDecimal("200"),
                    new BigDecimal("1000000")
            );
            assertEquals(new BigDecimal("0.00"), result);
        }
    }

    @Nested
    @DisplayName("calculateMarginRate - 안전마진율")
    class CalculateMarginRateTest {

        @Test
        @DisplayName("정상: 실제판매량 400, BEP 200 → 안전마진율 50%")
        void shouldReturnCorrectMarginRate() {
            BigDecimal result = calculator.calculateMarginRate(
                    new BigDecimal("400"),
                    new BigDecimal("200")
            );
            assertEquals(new BigDecimal("50.00"), result);
        }

        @Test
        @DisplayName("실제판매량 0 이하 → 0% 반환")
        void shouldReturnZeroWhenQuantityZero() {
            BigDecimal result = calculator.calculateMarginRate(
                    BigDecimal.ZERO,
                    new BigDecimal("200")
            );
            assertEquals(new BigDecimal("0.00"), result);
        }
    }

    @Nested
    @DisplayName("applyMinimumWage - 최저임금 하한 적용")
    class ApplyMinimumWageTest {

        @Test
        @DisplayName("시급 5000원 → 9860원으로 강제 적용")
        void shouldApplyMinimumWhenBelow() {
            BigDecimal result = calculator.applyMinimumWage(new BigDecimal("5000"));
            assertEquals(FinancialCalculator.MINIMUM_WAGE, result);
        }

        @Test
        @DisplayName("시급 9860원 → 그대로 유지")
        void shouldKeepWhenExact() {
            BigDecimal result = calculator.applyMinimumWage(new BigDecimal("9860"));
            assertEquals(new BigDecimal("9860"), result);
        }

        @Test
        @DisplayName("시급 15000원 → 그대로 유지")
        void shouldKeepWhenAbove() {
            BigDecimal result = calculator.applyMinimumWage(new BigDecimal("15000"));
            assertEquals(new BigDecimal("15000"), result);
        }

        @Test
        @DisplayName("시급 0원 → 9860원으로 강제 적용")
        void shouldApplyMinimumWhenZero() {
            BigDecimal result = calculator.applyMinimumWage(BigDecimal.ZERO);
            assertEquals(FinancialCalculator.MINIMUM_WAGE, result);
        }
    }

    @Nested
    @DisplayName("classifyZone - 안전마진 Zone 분류")
    class ClassifyZoneTest {

        @Test
        @DisplayName("안전마진율 50% → GREEN")
        void shouldReturnGreenWhenHigh() {
            assertEquals("GREEN", calculator.classifyZone(new BigDecimal("50.00")));
        }

        @Test
        @DisplayName("안전마진율 20.01% → GREEN")
        void shouldReturnGreenJustAboveThreshold() {
            assertEquals("GREEN", calculator.classifyZone(new BigDecimal("20.01")));
        }

        @Test
        @DisplayName("안전마진율 20.00% → YELLOW")
        void shouldReturnYellowAtThreshold() {
            assertEquals("YELLOW", calculator.classifyZone(new BigDecimal("20.00")));
        }

        @Test
        @DisplayName("안전마진율 10% → YELLOW")
        void shouldReturnYellowWhenModerate() {
            assertEquals("YELLOW", calculator.classifyZone(new BigDecimal("10.00")));
        }

        @Test
        @DisplayName("안전마진율 0% → YELLOW")
        void shouldReturnYellowAtZero() {
            assertEquals("YELLOW", calculator.classifyZone(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("안전마진율 -5% → RED")
        void shouldReturnRedWhenNegative() {
            assertEquals("RED", calculator.classifyZone(new BigDecimal("-5.00")));
        }
    }
}
