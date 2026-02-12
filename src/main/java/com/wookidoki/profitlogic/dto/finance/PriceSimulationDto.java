package com.wookidoki.profitlogic.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceSimulationDto {

    private BigDecimal currentPrice;
    private BigDecimal newPrice;
    private BigDecimal priceChangeRate;
    private BigDecimal elasticity;
    private BigDecimal expectedDemandChange;
    private BigDecimal expectedRevenue;
    private BigDecimal expectedProfit;
    private BigDecimal profitChangeRate;
}
