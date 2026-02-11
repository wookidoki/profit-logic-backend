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
}
