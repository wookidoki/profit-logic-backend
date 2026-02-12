package com.wookidoki.profitlogic.dto;

import com.wookidoki.profitlogic.domain.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {

    private Long id;
    private String title;
    private BigDecimal price;
    private BigDecimal variableCost;
    private BigDecimal fixedCost;
    private Integer workHours;
    private BigDecimal hourlyWage;
    private Boolean isPublic;
    private BigDecimal targetRevenue;
    private String targetMonth;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .price(project.getPrice())
                .variableCost(project.getVariableCost())
                .fixedCost(project.getFixedCost())
                .workHours(project.getWorkHours())
                .hourlyWage(project.getHourlyWage())
                .isPublic(project.getIsPublic())
                .targetRevenue(project.getTargetRevenue())
                .targetMonth(project.getTargetMonth())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
