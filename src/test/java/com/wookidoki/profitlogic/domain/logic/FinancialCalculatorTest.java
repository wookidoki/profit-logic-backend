package com.wookidoki.profitlogic.domain.logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FinancialCalculatorTest {

    private FinancialCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new FinancialCalculator();
    }

    @Test
    @DisplayName("BEP 계산: 50000 / (1000 - 100) = 55.56")
    void calculateBep() {
        BigDecimal result = calculator.calculateBep(
                new BigDecimal("50000"),
                new BigDecimal("1000"),
                new BigDecimal("100")
        );
        assertEquals(new BigDecimal("55.56"), result);
    }

    @Test
    @DisplayName("BEP 계산: 공헌이익 <= 0 이면 ArithmeticException")
    void calculateBep_throwsWhenContributionNonPositive() {
        assertThrows(ArithmeticException.class, () ->
                calculator.calculateBep(
                        new BigDecimal("50000"),
                        new BigDecimal("100"),
                        new BigDecimal("200")
                )
        );
    }

    @Test
    @DisplayName("영업이익: (1000 - 100) * 100 - 50000 = 40000")
    void calculateOperatingProfit() {
        BigDecimal result = calculator.calculateOperatingProfit(
                new BigDecimal("1000"),
                new BigDecimal("100"),
                new BigDecimal("100"),
                new BigDecimal("50000")
        );
        assertEquals(new BigDecimal("40000.00"), result);
    }

    @Test
    @DisplayName("경제적 이윤: 40000 - (10 * 10000) = -60000")
    void calculateEconomicProfit() {
        BigDecimal result = calculator.calculateEconomicProfit(
                new BigDecimal("40000"),
                new BigDecimal("10"),
                new BigDecimal("10000")
        );
        assertEquals(new BigDecimal("-60000.00"), result);
    }

    @Test
    @DisplayName("목표 판매량: (50000 + 1000000) / (1000 - 100) = 1166.67")
    void calculateTargetQuantity() {
        BigDecimal result = calculator.calculateTargetQuantity(
                new BigDecimal("50000"),
                new BigDecimal("1000000"),
                new BigDecimal("1000"),
                new BigDecimal("100")
        );
        assertEquals(new BigDecimal("1166.67"), result);
    }

    @Test
    @DisplayName("안전마진율: (1166.67 - 55.56) / 1166.67 * 100 = 95.24")
    void calculateMarginRate() {
        BigDecimal result = calculator.calculateMarginRate(
                new BigDecimal("1166.67"),
                new BigDecimal("55.56")
        );
        assertEquals(new BigDecimal("95.24"), result);
    }
}
