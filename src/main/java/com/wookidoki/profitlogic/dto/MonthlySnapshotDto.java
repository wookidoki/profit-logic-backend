package com.wookidoki.profitlogic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Builder
public class MonthlySnapshotDto {

    private String month;
    private BigDecimal totalCost;
    private BigDecimal totalHours;
    private BigDecimal shadowWage;
    private BigDecimal bepQuantity;
    private BigDecimal safetyMarginRatio;
}
