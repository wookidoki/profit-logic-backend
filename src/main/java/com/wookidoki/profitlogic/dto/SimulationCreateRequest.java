package com.wookidoki.profitlogic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationCreateRequest {

    @NotNull(message = "프로젝트 ID는 필수입니다.")
    private Long projectId;

    @NotBlank(message = "시나리오 이름은 필수입니다.")
    private String scenarioName;

    @NotBlank(message = "결과 JSON은 필수입니다.")
    private String resultJson;
}
