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
public class BepDto {

    private BigDecimal enhancedFixedCost;
    private BigDecimal bep;
    private BigDecimal price;
    private BigDecimal variableCostPerUnit;
    private BigDecimal contributionMargin;
}
