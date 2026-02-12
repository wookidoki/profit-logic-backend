package com.wookidoki.profitlogic.dto.ai;

import com.wookidoki.profitlogic.dto.finance.CalculateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiParseResponse {

    private BigDecimal price;
    private BigDecimal variableCost;
    private BigDecimal fixedCost;
    private Integer workHours;
    private BigDecimal hourlyWage;
    private BigDecimal targetProfit;
    private String detectedCategory;
    private String suggestion;

    public static AiParseResponse from(CalculateRequest req, String detectedCategory, String suggestion) {
        return AiParseResponse.builder()
                .price(req.getPrice())
                .variableCost(req.getVariableCost())
                .fixedCost(req.getFixedCost())
                .workHours(req.getWorkHours())
                .hourlyWage(req.getHourlyWage())
                .targetProfit(req.getTargetProfit())
                .detectedCategory(detectedCategory)
                .suggestion(suggestion)
                .build();
    }
}
