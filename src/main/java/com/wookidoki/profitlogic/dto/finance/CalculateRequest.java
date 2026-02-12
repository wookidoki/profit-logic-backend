package com.wookidoki.profitlogic.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateRequest {

    @NotNull(message = "판매가(price)는 필수입니다.")
    @DecimalMin(value = "0", message = "판매가는 0 이상이어야 합니다.")
    private BigDecimal price;

    @NotNull(message = "변동비(variableCost)는 필수입니다.")
    @DecimalMin(value = "0", message = "변동비는 0 이상이어야 합니다.")
    private BigDecimal variableCost;

    @NotNull(message = "고정비(fixedCost)는 필수입니다.")
    @DecimalMin(value = "0", message = "고정비는 0 이상이어야 합니다.")
    private BigDecimal fixedCost;

    @NotNull(message = "근무시간(workHours)은 필수입니다.")
    @Min(value = 0, message = "근무시간은 0 이상이어야 합니다.")
    private Integer workHours;

    @NotNull(message = "시급(hourlyWage)은 필수입니다.")
    @DecimalMin(value = "0", message = "시급은 0 이상이어야 합니다.")
    private BigDecimal hourlyWage;

    @NotNull(message = "목표이익(targetProfit)은 필수입니다.")
    @DecimalMin(value = "0", message = "목표이익은 0 이상이어야 합니다.")
    private BigDecimal targetProfit;
}
