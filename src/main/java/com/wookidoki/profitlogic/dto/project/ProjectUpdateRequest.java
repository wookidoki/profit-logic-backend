package com.wookidoki.profitlogic.dto.project;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class ProjectUpdateRequest {

    @NotBlank(message = "프로젝트 제목은 필수입니다.")
    private String title;

    @NotNull(message = "판매가는 필수입니다.")
    @DecimalMin(value = "0", message = "판매가는 0 이상이어야 합니다.")
    private BigDecimal price;

    @NotNull(message = "변동비는 필수입니다.")
    @DecimalMin(value = "0", message = "변동비는 0 이상이어야 합니다.")
    private BigDecimal variableCost;

    @NotNull(message = "고정비는 필수입니다.")
    @DecimalMin(value = "0", message = "고정비는 0 이상이어야 합니다.")
    private BigDecimal fixedCost;

    @NotNull(message = "근무시간은 필수입니다.")
    @Min(value = 1, message = "근무시간은 1 이상이어야 합니다.")
    private Integer workHours;

    @NotNull(message = "시급은 필수입니다.")
    @DecimalMin(value = "0", message = "시급은 0 이상이어야 합니다.")
    private BigDecimal hourlyWage;

    @NotNull(message = "공개여부는 필수입니다.")
    private Boolean isPublic;

    @DecimalMin(value = "0", message = "목표 매출은 0 이상이어야 합니다.")
    private BigDecimal targetRevenue;

    private String targetMonth;
}
