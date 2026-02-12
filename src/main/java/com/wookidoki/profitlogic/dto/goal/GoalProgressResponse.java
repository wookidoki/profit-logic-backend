package com.wookidoki.profitlogic.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Builder
public class GoalProgressResponse {

    private BigDecimal targetRevenue;
    private String targetMonth;

    private int monthsTotal;
    private int monthsElapsed;
    private int monthsRemaining;
    private BigDecimal timeProgressPercent;

    private BigDecimal bepQuantity;
    private BigDecimal requiredMonthlySales;
    private BigDecimal dailySalesTarget;

    private BigDecimal currentShadowWage;
    private BigDecimal monthlyCostAverage;

    private String status;
}
