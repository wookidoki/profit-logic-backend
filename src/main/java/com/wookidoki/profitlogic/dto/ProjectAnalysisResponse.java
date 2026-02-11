package com.wookidoki.profitlogic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAnalysisResponse {

    private Long projectId;
    private String projectTitle;
    private CostBreakdownDto costBreakdown;
    private ShadowWageDto shadowWage;
    private BepDto bep;
    private List<ActionCardDto> actionCards;
}
