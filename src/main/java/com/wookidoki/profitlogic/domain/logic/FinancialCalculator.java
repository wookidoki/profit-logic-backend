package com.wookidoki.profitlogic.domain.logic;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 핵심 금융 계산 엔진.
 * 모든 연산은 BigDecimal로 수행. double/float 사용 금지.
 */
@Component
public class FinancialCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * 손익분기점(BEP) = FixedCost / (Price - VariableCost)
     * 결과는 소수점 첫째 자리에서 올림(CEILING)하여 정수로 반환.
     *
     * @throws ArithmeticException (Price - VariableCost) <= 0 인 경우
     */
    public BigDecimal calculateBEP(BigDecimal fixedCost, BigDecimal price, BigDecimal variableCost) {
        BigDecimal contribution = price.subtract(variableCost);

        if (contribution.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ArithmeticException("공헌이익이 0 이하입니다");
        }

        return fixedCost.divide(contribution, 0, RoundingMode.CEILING);
    }

    /**
     * 영업이익 = (Price - VariableCost) * Quantity - FixedCost
     */
    public BigDecimal calculateOperatingProfit(BigDecimal price, BigDecimal variableCost,
                                                BigDecimal quantity, BigDecimal fixedCost) {
        BigDecimal contribution = price.subtract(variableCost);
        return contribution.multiply(quantity).subtract(fixedCost).setScale(SCALE, ROUNDING);
    }

    /**
     * 경제적 이윤 = OperatingProfit - (WorkHours * HourlyWage)
     * 사용자의 인건비(기회비용)를 뺀 진짜 이익.
     */
    public BigDecimal calculateEconomicProfit(BigDecimal operatingProfit,
                                               BigDecimal workHours, BigDecimal hourlyWage) {
        BigDecimal opportunityCost = workHours.multiply(hourlyWage);
        return operatingProfit.subtract(opportunityCost).setScale(SCALE, ROUNDING);
    }

    /**
     * 목표 판매량 = (FixedCost + TargetProfit) / (Price - VariableCost)
     *
     * @throws ArithmeticException (Price - VariableCost) <= 0 인 경우
     */
    public BigDecimal calculateTargetSales(BigDecimal fixedCost, BigDecimal targetProfit,
                                            BigDecimal price, BigDecimal variableCost) {
        BigDecimal contribution = price.subtract(variableCost);

        if (contribution.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ArithmeticException("공헌이익이 0 이하입니다");
        }

        return fixedCost.add(targetProfit).divide(contribution, SCALE, ROUNDING);
    }

    /**
     * 안전마진율(%) = (실제판매량 - BEP) / 실제판매량 * 100
     */
    public BigDecimal calculateMarginRate(BigDecimal actualQuantity, BigDecimal bep) {
        if (actualQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }

        return actualQuantity.subtract(bep)
                .divide(actualQuantity, SCALE + 2, ROUNDING)
                .multiply(new BigDecimal("100"))
                .setScale(SCALE, ROUNDING);
    }
}
