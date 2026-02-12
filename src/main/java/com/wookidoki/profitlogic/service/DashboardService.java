package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.dto.*;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final ProjectRepository projectRepository;
    private final ProjectAnalysisService analysisService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId) {
        List<Project> projects = projectRepository.findByUserId(userId);

        if (projects.isEmpty()) {
            return DashboardSummaryResponse.builder()
                    .totalProjects(0)
                    .totalEstimatedRevenue(BigDecimal.ZERO)
                    .avgShadowWage(BigDecimal.ZERO)
                    .avgContributionMarginRate(BigDecimal.ZERO)
                    .warningCount(0)
                    .projects(List.of())
                    .build();
        }

        List<ProjectInsightDto> insights = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalShadowWage = BigDecimal.ZERO;
        BigDecimal totalMarginRate = BigDecimal.ZERO;
        int analyzedCount = 0;
        int warningCount = 0;

        for (Project project : projects) {
            try {
                ProjectAnalysisResponse analysis = analysisService.analyzeProject(
                        project.getId(), userId);

                BigDecimal bep = analysis.getBep().getBep();
                BigDecimal shadowWage = analysis.getShadowWage().getRealShadowWage();
                BigDecimal contribution = analysis.getBep().getContributionMargin();
                BigDecimal price = analysis.getBep().getPrice();

                BigDecimal marginRate = price.compareTo(BigDecimal.ZERO) > 0
                        ? contribution.divide(price, SCALE + 2, ROUNDING)
                            .multiply(new BigDecimal("100")).setScale(SCALE, ROUNDING)
                        : BigDecimal.ZERO;

                // Determine status from action cards
                String status = determineStatus(analysis.getActionCards());

                // Find top priority action card
                ActionCardDto topCard = analysis.getActionCards().stream()
                        .min(Comparator.comparingInt(ActionCardDto::getPriority))
                        .orElse(null);

                if ("DANGER".equals(status) || "WARNING".equals(status)) {
                    warningCount++;
                }

                insights.add(ProjectInsightDto.builder()
                        .projectId(project.getId())
                        .title(project.getTitle())
                        .bep(bep)
                        .shadowWage(shadowWage)
                        .contributionMarginRate(marginRate)
                        .status(status)
                        .topActionCard(topCard)
                        .creatorCategory(project.getCreatorCategory() != null
                                ? project.getCreatorCategory().name() : null)
                        .build());

                totalRevenue = totalRevenue.add(price.multiply(bep));
                totalShadowWage = totalShadowWage.add(shadowWage);
                totalMarginRate = totalMarginRate.add(marginRate);
                analyzedCount++;

            } catch (Exception e) {
                log.warn("프로젝트 {} 분석 실패: {}", project.getId(), e.getMessage());
                insights.add(ProjectInsightDto.builder()
                        .projectId(project.getId())
                        .title(project.getTitle())
                        .bep(BigDecimal.ZERO)
                        .shadowWage(BigDecimal.ZERO)
                        .contributionMarginRate(BigDecimal.ZERO)
                        .status("NO_DATA")
                        .topActionCard(null)
                        .creatorCategory(project.getCreatorCategory() != null
                                ? project.getCreatorCategory().name() : null)
                        .build());
            }
        }

        BigDecimal avgWage = analyzedCount > 0
                ? totalShadowWage.divide(new BigDecimal(analyzedCount), SCALE, ROUNDING)
                : BigDecimal.ZERO;
        BigDecimal avgMargin = analyzedCount > 0
                ? totalMarginRate.divide(new BigDecimal(analyzedCount), SCALE, ROUNDING)
                : BigDecimal.ZERO;

        return DashboardSummaryResponse.builder()
                .totalProjects(projects.size())
                .totalEstimatedRevenue(totalRevenue.setScale(SCALE, ROUNDING))
                .avgShadowWage(avgWage)
                .avgContributionMarginRate(avgMargin)
                .warningCount(warningCount)
                .projects(insights)
                .build();
    }

    private String determineStatus(List<ActionCardDto> cards) {
        boolean hasWarning = cards.stream().anyMatch(c -> "WARNING".equals(c.getType()));
        boolean hasPositive = cards.stream().anyMatch(c -> "POSITIVE".equals(c.getType()));

        if (hasWarning && cards.stream().filter(c -> "WARNING".equals(c.getType())).count() >= 2) {
            return "DANGER";
        }
        if (hasWarning) {
            return "WARNING";
        }
        if (hasPositive) {
            return "STABLE";
        }
        return "NORMAL";
    }
}
