package com.wookidoki.profitlogic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private int totalProjects;
    private BigDecimal totalEstimatedRevenue;
    private BigDecimal avgShadowWage;
    private BigDecimal avgContributionMarginRate;
    private int warningCount;
    private List<ProjectInsightDto> projects;
}
