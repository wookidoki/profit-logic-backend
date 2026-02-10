package com.wookidoki.profitlogic.domain.logic;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 핵심 금융 계산 엔진.
 * 모든 연산은 BigDecimal로 수행하며, 반올림은 HALF_UP, 소수점 이하 2자리 기준.
 */
@Component
public class FinancialCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * 손익분기점(BEP) = FixedCost / (Price - VariableCost)
     *
     * @throws ArithmeticException (Price - VariableCost) <= 0 인 경우
     */
    public BigDecimal calculateBep(BigDecimal fixedCost, BigDecimal price, BigDecimal variableCost) {
        BigDecimal contribution = price.subtract(variableCost);

        if (contribution.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ArithmeticException(
                    "공헌이익(판매가 - 변동비)이 0 이하입니다. 판매가가 변동비보다 커야 합니다. " +
                    "현재 공헌이익: " + contribution
            );
        }

        return fixedCost.divide(contribution, SCALE, ROUNDING);
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
     * 사용자의 인건비(기회비용)를 뺀 진짜 이익
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
    public BigDecimal calculateTargetQuantity(BigDecimal fixedCost, BigDecimal targetProfit,
                                               BigDecimal price, BigDecimal variableCost) {
        BigDecimal contribution = price.subtract(variableCost);

        if (contribution.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ArithmeticException(
                    "공헌이익(판매가 - 변동비)이 0 이하입니다. 판매가가 변동비보다 커야 합니다. " +
                    "현재 공헌이익: " + contribution
            );
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
