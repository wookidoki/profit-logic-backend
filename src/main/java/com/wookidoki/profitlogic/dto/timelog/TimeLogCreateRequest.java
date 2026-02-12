package com.wookidoki.profitlogic.dto.timelog;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeLogCreateRequest {

    @NotBlank(message = "작업명은 필수입니다.")
    @Size(max = 100)
    private String taskName;

    @NotNull(message = "작업 시간은 필수입니다.")
    @DecimalMin(value = "0.1", message = "작업 시간은 0.1시간 이상이어야 합니다.")
    @DecimalMax(value = "24", message = "작업 시간은 24시간을 초과할 수 없습니다.")
    private BigDecimal hoursSpent;

    @NotNull(message = "작업 날짜는 필수입니다.")
    private LocalDate logDate;

    @Size(max = 500)
    private String memo;
}
