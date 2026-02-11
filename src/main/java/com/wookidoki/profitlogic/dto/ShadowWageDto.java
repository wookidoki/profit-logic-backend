package com.wookidoki.profitlogic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShadowWageDto {

    private BigDecimal totalHours;
    private BigDecimal operatingProfit;
    private BigDecimal realShadowWage;
    private BigDecimal minimumWage;
    private BigDecimal minimumWageRatio;
    private boolean hasTimeData;
}
