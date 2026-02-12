package com.wookidoki.profitlogic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Builder
public class ProjectInsightDto {

    private Long projectId;
    private String title;
    private BigDecimal bep;
    private BigDecimal shadowWage;
    private BigDecimal contributionMarginRate;
    private String status;
    private ActionCardDto topActionCard;
    private String creatorCategory;
}
