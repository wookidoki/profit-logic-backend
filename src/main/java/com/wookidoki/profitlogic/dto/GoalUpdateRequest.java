package com.wookidoki.profitlogic.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalUpdateRequest {

    @DecimalMin(value = "0", message = "목표 매출은 0 이상이어야 합니다.")
    private BigDecimal targetRevenue;

    private String targetMonth;
}
