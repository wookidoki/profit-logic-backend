package com.wookidoki.profitlogic.domain.logic;

import com.wookidoki.profitlogic.common.exception.BusinessLogicException;
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

    /** 2025년 기준 최저시급 */
    public static final BigDecimal MINIMUM_WAGE = new BigDecimal("9860");

    /**
     * 시급에 최저임금 하한을 적용한다.
     * hourlyWage < 9,860원이면 9,860원으로 강제 적용.
     */
    public BigDecimal applyMinimumWage(BigDecimal hourlyWage) {
        if (hourlyWage.compareTo(MINIMUM_WAGE) < 0) {
            return MINIMUM_WAGE;
        }
        return hourlyWage;
    }

    /**
     * 손익분기점(BEP) = FixedCost / (Price - VariableCost)
     * 결과는 소수점 첫째 자리에서 올림(CEILING)하여 정수로 반환.
     */
    public BigDecimal calculateBEP(BigDecimal fixedCost, BigDecimal price, BigDecimal variableCost) {
        BigDecimal contribution = price.subtract(variableCost);
        validateContribution(contribution);
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
     */
    public BigDecimal calculateEconomicProfit(BigDecimal operatingProfit,
                                               BigDecimal workHours, BigDecimal hourlyWage) {
        BigDecimal opportunityCost = workHours.multiply(hourlyWage);
        return operatingProfit.subtract(opportunityCost).setScale(SCALE, ROUNDING);
    }

    /**
     * 실질 시급 = OperatingProfit / WorkHours
     */
    public BigDecimal calculateShadowWage(BigDecimal operatingProfit, BigDecimal workHours) {
        if (workHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessLogicException("근무시간이 0 이하입니다");
        }
        return operatingProfit.divide(workHours, SCALE, ROUNDING);
    }

    /**
     * 목표 판매량 = (FixedCost + TargetProfit) / (Price - VariableCost)
     */
    public BigDecimal calculateTargetSales(BigDecimal fixedCost, BigDecimal targetProfit,
                                            BigDecimal price, BigDecimal variableCost) {
        BigDecimal contribution = price.subtract(variableCost);
        validateContribution(contribution);
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

    /**
     * 안전마진 Zone 분류.
     * - GREEN: 안전마진율 > 20% (안전 구간)
     * - YELLOW: 0% <= 안전마진율 <= 20% (주의 구간)
     * - RED: 안전마진율 < 0% (위험 구간)
     */
    public String classifyZone(BigDecimal marginRate) {
        if (marginRate.compareTo(new BigDecimal("20")) > 0) {
            return "GREEN";
        } else if (marginRate.compareTo(BigDecimal.ZERO) >= 0) {
            return "YELLOW";
        } else {
            return "RED";
        }
    }

    private void validateContribution(BigDecimal contribution) {
        if (contribution.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessLogicException("팔수록 손해입니다");
        }
    }
}
