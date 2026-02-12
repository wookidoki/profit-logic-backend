package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.CostDetail;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.TimeLog;
import com.wookidoki.profitlogic.domain.logic.FinancialCalculator;
import com.wookidoki.profitlogic.dto.goal.GoalProgressResponse;
import com.wookidoki.profitlogic.dto.goal.GoalUpdateRequest;
import com.wookidoki.profitlogic.dto.project.ProjectResponse;
import com.wookidoki.profitlogic.repository.CostDetailRepository;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.TimeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal DAYS_PER_MONTH = new BigDecimal("30");

    private final ProjectRepository projectRepository;
    private final CostDetailRepository costDetailRepository;
    private final TimeLogRepository timeLogRepository;
    private final FinancialCalculator calculator;

    @Transactional(readOnly = true)
    public GoalProgressResponse getProgress(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", projectId));

        if (!project.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }

        BigDecimal targetRevenue = project.getTargetRevenue();
        String targetMonth = project.getTargetMonth();

        if (targetRevenue == null || targetMonth == null) {
            return GoalProgressResponse.builder()
                    .status("NO_TARGET")
                    .build();
        }

        YearMonth startYm = YearMonth.from(project.getCreatedAt().toLocalDate());
        YearMonth targetYm = YearMonth.parse(targetMonth);
        YearMonth currentYm = YearMonth.now();

        int monthsTotal = (int) startYm.until(targetYm, ChronoUnit.MONTHS);
        if (monthsTotal <= 0) monthsTotal = 1;

        int monthsElapsed = (int) startYm.until(currentYm, ChronoUnit.MONTHS);
        if (monthsElapsed < 0) monthsElapsed = 0;
        if (monthsElapsed > monthsTotal) monthsElapsed = monthsTotal;

        int monthsRemaining = monthsTotal - monthsElapsed;
        if (monthsRemaining < 0) monthsRemaining = 0;

        BigDecimal timeProgress = new BigDecimal(monthsElapsed)
                .divide(new BigDecimal(monthsTotal), SCALE + 2, ROUNDING)
                .multiply(new BigDecimal("100"))
                .setScale(SCALE, ROUNDING);

        // BEP calculation
        BigDecimal price = project.getPrice();
        BigDecimal variableCost = project.getVariableCost();
        BigDecimal fixedCost = project.getFixedCost();
        BigDecimal contribution = price.subtract(variableCost);

        BigDecimal bep = BigDecimal.ZERO;
        BigDecimal requiredMonthlySales = BigDecimal.ZERO;
        BigDecimal dailySalesTarget = BigDecimal.ZERO;

        if (contribution.compareTo(BigDecimal.ZERO) > 0) {
            bep = calculator.calculateBEP(fixedCost, price, variableCost);

            // Total units needed to reach target revenue
            BigDecimal totalUnitsNeeded = targetRevenue.divide(price, SCALE, ROUNDING);

            if (monthsRemaining > 0) {
                requiredMonthlySales = totalUnitsNeeded
                        .divide(new BigDecimal(monthsRemaining), SCALE, ROUNDING);
                dailySalesTarget = requiredMonthlySales
                        .divide(DAYS_PER_MONTH, SCALE, ROUNDING);
            }
        }

        // Average monthly cost from actual data
        List<CostDetail> allCosts = costDetailRepository.findByProjectId(projectId);
        BigDecimal totalCost = allCosts.stream()
                .map(CostDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int dataMonths = Math.max(monthsElapsed, 1);
        BigDecimal monthlyCostAverage = totalCost.divide(
                new BigDecimal(dataMonths), SCALE, ROUNDING);

        // Current shadow wage from actual time data
        List<TimeLog> allLogs = timeLogRepository.findByProjectId(projectId);
        BigDecimal totalHours = allLogs.stream()
                .map(TimeLog::getHoursSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal projectedIncome = project.getHourlyWage()
                .multiply(new BigDecimal(project.getWorkHours()));
        BigDecimal currentShadowWage = BigDecimal.ZERO;
        if (totalHours.compareTo(BigDecimal.ZERO) > 0) {
            currentShadowWage = projectedIncome.divide(totalHours, SCALE, ROUNDING);
        } else {
            currentShadowWage = project.getHourlyWage();
        }

        // Determine status
        String status = determineStatus(timeProgress, monthsRemaining, bep, requiredMonthlySales);

        return GoalProgressResponse.builder()
                .targetRevenue(targetRevenue.setScale(SCALE, ROUNDING))
                .targetMonth(targetMonth)
                .monthsTotal(monthsTotal)
                .monthsElapsed(monthsElapsed)
                .monthsRemaining(monthsRemaining)
                .timeProgressPercent(timeProgress)
                .bepQuantity(bep)
                .requiredMonthlySales(requiredMonthlySales)
                .dailySalesTarget(dailySalesTarget)
                .currentShadowWage(currentShadowWage)
                .monthlyCostAverage(monthlyCostAverage)
                .status(status)
                .build();
    }

    @Transactional
    public ProjectResponse updateGoal(Long projectId, Long userId, GoalUpdateRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", projectId));

        if (!project.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }

        project.updateGoal(request.getTargetRevenue(), request.getTargetMonth());
        return ProjectResponse.from(project);
    }

    private String determineStatus(BigDecimal timeProgress, int monthsRemaining,
                                    BigDecimal bep, BigDecimal requiredMonthlySales) {
        if (monthsRemaining <= 0) {
            return "EXPIRED";
        }
        if (requiredMonthlySales.compareTo(BigDecimal.ZERO) <= 0) {
            return "ON_TRACK";
        }
        // If BEP exceeds monthly sales target, it's tough
        if (bep.compareTo(requiredMonthlySales) > 0) {
            return "BEHIND";
        }
        if (timeProgress.compareTo(new BigDecimal("70")) >= 0) {
            return "URGENT";
        }
        return "ON_TRACK";
    }
}
