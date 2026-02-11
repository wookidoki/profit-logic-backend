package com.wookidoki.profitlogic.dto;

import com.wookidoki.profitlogic.domain.Simulation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationResponse {

    private Long id;
    private Long projectId;
    private String scenarioName;
    private String resultJson;
    private LocalDateTime createdAt;

    public static SimulationResponse from(Simulation simulation) {
        return SimulationResponse.builder()
                .id(simulation.getId())
                .projectId(simulation.getProject().getId())
                .scenarioName(simulation.getScenarioName())
                .resultJson(simulation.getResultJson())
                .createdAt(simulation.getCreatedAt())
                .build();
    }
}
