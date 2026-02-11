package com.wookidoki.profitlogic.dto;

import com.wookidoki.profitlogic.domain.CostCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostBreakdownDto {

    private BigDecimal totalFixedCost;
    private BigDecimal totalVariableCost;
    private BigDecimal totalCost;
    private Map<CostCategory, BigDecimal> categoryRatio;
}
