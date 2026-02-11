package com.wookidoki.profitlogic.dto;

import com.wookidoki.profitlogic.domain.CostCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostDetailCreateRequest {

    @NotNull(message = "비용 카테고리는 필수입니다.")
    private CostCategory category;

    @NotBlank(message = "비용 항목명은 필수입니다.")
    @Size(max = 100)
    private String costName;

    @NotBlank(message = "비용 유형은 필수입니다.")
    private String costType;

    @NotNull(message = "금액은 필수입니다.")
    @DecimalMin(value = "0", inclusive = false, message = "금액은 0보다 커야 합니다.")
    private BigDecimal amount;

    @Size(max = 500)
    private String memo;
}
