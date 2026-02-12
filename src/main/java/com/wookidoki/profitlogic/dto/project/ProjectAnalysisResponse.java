package com.wookidoki.profitlogic.dto.project;

import com.wookidoki.profitlogic.dto.finance.ActionCardDto;
import com.wookidoki.profitlogic.dto.finance.BepDto;
import com.wookidoki.profitlogic.dto.finance.CostBreakdownDto;
import com.wookidoki.profitlogic.dto.finance.ShadowWageDto;
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
