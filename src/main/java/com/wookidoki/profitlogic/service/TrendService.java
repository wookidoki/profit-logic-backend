package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.CostDetail;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.TimeLog;
import com.wookidoki.profitlogic.domain.logic.FinancialCalculator;
import com.wookidoki.profitlogic.dto.trend.MonthlySnapshotDto;
import com.wookidoki.profitlogic.dto.trend.MonthlyTrendResponse;
import com.wookidoki.profitlogic.repository.CostDetailRepository;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.TimeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrendService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final ProjectRepository projectRepository;
    private final CostDetailRepository costDetailRepository;
    private final TimeLogRepository timeLogRepository;
    private final FinancialCalculator calculator;

    @Transactional(readOnly = true)
    public MonthlyTrendResponse getMonthlyTrends(Long projectId, Long userId, int months) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", projectId));

        if (!project.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }

        YearMonth current = YearMonth.now();
        List<String> monthLabels = new ArrayList<>();
        List<MonthlySnapshotDto> snapshots = new ArrayList<>();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            String monthStr = ym.toString();
            monthLabels.add(monthStr);

            MonthlySnapshotDto snapshot = buildSnapshot(project, projectId, ym);
            snapshots.add(snapshot);
        }

        return MonthlyTrendResponse.builder()
                .months(monthLabels)
                .snapshots(snapshots)
                .build();
    }

    private MonthlySnapshotDto buildSnapshot(Project project, Long projectId, YearMonth ym) {
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);
        LocalDate dateStart = ym.atDay(1);
        LocalDate dateEnd = ym.atEndOfMonth();

        // 해당 월 비용 집계
        List<CostDetail> monthlyCosts = costDetailRepository
                .findByProjectIdAndCreatedAtBetween(projectId, monthStart, monthEnd);

        BigDecimal monthlyFixedCost = BigDecimal.ZERO;
        BigDecimal monthlyVariableCost = BigDecimal.ZERO;

        for (CostDetail cost : monthlyCosts) {
            if ("FIXED".equals(cost.getCostType())) {
                monthlyFixedCost = monthlyFixedCost.add(cost.getAmount());
            } else {
                monthlyVariableCost = monthlyVariableCost.add(cost.getAmount());
            }
        }

        BigDecimal totalCost = monthlyFixedCost.add(monthlyVariableCost);

        // 해당 월 시간 집계
        List<TimeLog> monthlyLogs = timeLogRepository
                .findByProjectIdAndLogDateBetween(projectId, dateStart, dateEnd);

        BigDecimal totalHours = monthlyLogs.stream()
                .map(TimeLog::getHoursSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 데이터가 없는 월은 프로젝트 기본값 사용
        BigDecimal effectiveFixedCost = monthlyFixedCost.compareTo(BigDecimal.ZERO) > 0
                ? monthlyFixedCost : project.getFixedCost();
        BigDecimal effectiveHours = totalHours.compareTo(BigDecimal.ZERO) > 0
                ? totalHours : new BigDecimal(project.getWorkHours());

        // BEP 계산
        BigDecimal price = project.getPrice();
        BigDecimal variableCostPerUnit = project.getVariableCost();
        BigDecimal contribution = price.subtract(variableCostPerUnit);

        BigDecimal bep;
        BigDecimal shadowWage;
        BigDecimal safetyMargin;

        if (contribution.compareTo(BigDecimal.ZERO) <= 0) {
            bep = BigDecimal.ZERO;
            shadowWage = BigDecimal.ZERO;
            safetyMargin = BigDecimal.ZERO;
        } else {
            bep = calculator.calculateBEP(effectiveFixedCost, price, variableCostPerUnit);

            // 영업이익 = hourlyWage * workHours (프로젝트 기대 수입)
            BigDecimal projectedIncome = project.getHourlyWage()
                    .multiply(new BigDecimal(project.getWorkHours()));

            // 실질시급 = projectedIncome / effectiveHours
            shadowWage = effectiveHours.compareTo(BigDecimal.ZERO) > 0
                    ? projectedIncome.divide(effectiveHours, SCALE, ROUNDING)
                    : BigDecimal.ZERO;

            // 안전마진율 = (targetQuantity - BEP) / targetQuantity * 100
            BigDecimal targetQuantity = calculator.calculateTargetSales(
                    effectiveFixedCost, projectedIncome, price, variableCostPerUnit);
            safetyMargin = calculator.calculateMarginRate(targetQuantity, bep);
        }

        return MonthlySnapshotDto.builder()
                .month(ym.toString())
                .totalCost(totalCost.setScale(SCALE, ROUNDING))
                .totalHours(totalHours.setScale(SCALE, ROUNDING))
                .shadowWage(shadowWage.setScale(SCALE, ROUNDING))
                .bepQuantity(bep)
                .safetyMarginRatio(safetyMargin)
                .build();
    }
}
