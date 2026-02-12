package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.BusinessLogicException;
import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.CostCategory;
import com.wookidoki.profitlogic.domain.CostDetail;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.TimeLog;
import com.wookidoki.profitlogic.domain.logic.FinancialCalculator;
import com.wookidoki.profitlogic.dto.finance.ActionCardDto;
import com.wookidoki.profitlogic.dto.finance.BepDto;
import com.wookidoki.profitlogic.dto.finance.CostBreakdownDto;
import com.wookidoki.profitlogic.dto.finance.PriceSimulationDto;
import com.wookidoki.profitlogic.dto.finance.ShadowWageDto;
import com.wookidoki.profitlogic.dto.project.ProjectAnalysisResponse;
import com.wookidoki.profitlogic.repository.CostDetailRepository;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.TimeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectAnalysisService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal MINIMUM_WAGE = new BigDecimal("9860");
    private static final BigDecimal DEFAULT_ELASTICITY = new BigDecimal("-1.5");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ProjectRepository projectRepository;
    private final CostDetailRepository costDetailRepository;
    private final TimeLogRepository timeLogRepository;
    private final FinancialCalculator calculator;

    @Transactional(readOnly = true)
    public ProjectAnalysisResponse analyzeProject(Long projectId, Long userId) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);

        CostBreakdownDto costBreakdown = calculateCostBreakdown(projectId);
        BepDto bep = calculateEnhancedBEP(projectId, project, costBreakdown.getTotalFixedCost());
        ShadowWageDto shadowWage = calculateRealShadowWage(projectId, project, costBreakdown.getTotalFixedCost());
        List<ActionCardDto> actionCards = generateActionCards(
                projectId, project, costBreakdown, shadowWage, bep);

        return ProjectAnalysisResponse.builder()
                .projectId(project.getId())
                .projectTitle(project.getTitle())
                .costBreakdown(costBreakdown)
                .shadowWage(shadowWage)
                .bep(bep)
                .actionCards(actionCards)
                .build();
    }

    @Transactional(readOnly = true)
    public PriceSimulationDto simulatePriceChange(Long projectId, Long userId,
                                                   BigDecimal newPrice, BigDecimal elasticity) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);

        if (elasticity == null) {
            elasticity = DEFAULT_ELASTICITY;
        }

        BigDecimal currentPrice = project.getPrice();
        if (currentPrice.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessLogicException("현재 판매가가 0원이므로 가격 시뮬레이션을 수행할 수 없습니다.");
        }
        BigDecimal variableCost = project.getVariableCost();
        BigDecimal enhancedFixedCost = getEnhancedFixedCost(projectId, project);
        BigDecimal contribution = currentPrice.subtract(variableCost);

        BigDecimal currentQuantity = calculator.calculateTargetSales(
                enhancedFixedCost,
                project.getHourlyWage().multiply(new BigDecimal(project.getWorkHours())),
                currentPrice, variableCost);

        BigDecimal priceChangeRate = newPrice.subtract(currentPrice)
                .divide(currentPrice, SCALE + 2, ROUNDING)
                .multiply(HUNDRED).setScale(SCALE, ROUNDING);

        BigDecimal expectedDemandChange = priceChangeRate.multiply(elasticity)
                .setScale(SCALE, ROUNDING);

        BigDecimal demandMultiplier = BigDecimal.ONE
                .add(expectedDemandChange.divide(HUNDRED, SCALE + 2, ROUNDING));
        BigDecimal newQuantity = currentQuantity.multiply(demandMultiplier)
                .setScale(SCALE, ROUNDING);

        BigDecimal expectedRevenue = newPrice.multiply(newQuantity).setScale(SCALE, ROUNDING);
        BigDecimal expectedProfit = newPrice.subtract(variableCost)
                .multiply(newQuantity).subtract(enhancedFixedCost).setScale(SCALE, ROUNDING);

        BigDecimal currentProfit = contribution.multiply(currentQuantity)
                .subtract(enhancedFixedCost).setScale(SCALE, ROUNDING);

        BigDecimal profitChangeRate;
        if (currentProfit.compareTo(BigDecimal.ZERO) == 0) {
            profitChangeRate = null;
        } else {
            profitChangeRate = expectedProfit.subtract(currentProfit)
                    .divide(currentProfit.abs(), SCALE + 2, ROUNDING)
                    .multiply(HUNDRED).setScale(SCALE, ROUNDING);
        }

        return PriceSimulationDto.builder()
                .currentPrice(currentPrice)
                .newPrice(newPrice)
                .priceChangeRate(priceChangeRate)
                .elasticity(elasticity)
                .expectedDemandChange(expectedDemandChange)
                .expectedRevenue(expectedRevenue)
                .expectedProfit(expectedProfit)
                .profitChangeRate(profitChangeRate)
                .build();
    }

    CostBreakdownDto calculateCostBreakdown(Long projectId) {
        List<CostDetail> costs = costDetailRepository.findByProjectId(projectId);

        BigDecimal totalFixed = BigDecimal.ZERO;
        BigDecimal totalVariable = BigDecimal.ZERO;
        Map<CostCategory, BigDecimal> categoryAmounts = new EnumMap<>(CostCategory.class);

        for (CostDetail cost : costs) {
            if ("FIXED".equals(cost.getCostType())) {
                totalFixed = totalFixed.add(cost.getAmount());
            } else {
                totalVariable = totalVariable.add(cost.getAmount());
            }
            categoryAmounts.merge(cost.getCategory(), cost.getAmount(), BigDecimal::add);
        }

        BigDecimal totalCost = totalFixed.add(totalVariable);

        Map<CostCategory, BigDecimal> categoryRatio = new EnumMap<>(CostCategory.class);
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<CostCategory, BigDecimal> entry : categoryAmounts.entrySet()) {
                BigDecimal ratio = entry.getValue()
                        .divide(totalCost, SCALE + 2, ROUNDING)
                        .multiply(HUNDRED).setScale(SCALE, ROUNDING);
                categoryRatio.put(entry.getKey(), ratio);
            }
        }

        return CostBreakdownDto.builder()
                .totalFixedCost(totalFixed.setScale(SCALE, ROUNDING))
                .totalVariableCost(totalVariable.setScale(SCALE, ROUNDING))
                .totalCost(totalCost.setScale(SCALE, ROUNDING))
                .categoryRatio(categoryRatio)
                .build();
    }

    ShadowWageDto calculateRealShadowWage(Long projectId, Project project,
                                           BigDecimal enhancedFixedCost) {
        BigDecimal totalHours = getTotalHoursFromTimeLogs(projectId);
        boolean hasTimeData = totalHours.compareTo(BigDecimal.ZERO) > 0;

        if (!hasTimeData) {
            totalHours = new BigDecimal(project.getWorkHours());
        }

        BigDecimal effectiveFixedCost = enhancedFixedCost.compareTo(BigDecimal.ZERO) > 0
                ? enhancedFixedCost : project.getFixedCost();

        BigDecimal projectedIncome = project.getHourlyWage()
                .multiply(new BigDecimal(project.getWorkHours()));

        BigDecimal operatingProfit = projectedIncome;

        BigDecimal realShadowWage;
        BigDecimal minimumWageRatio;
        if (totalHours.compareTo(BigDecimal.ZERO) > 0) {
            realShadowWage = operatingProfit.divide(totalHours, SCALE, ROUNDING);
            minimumWageRatio = realShadowWage.divide(MINIMUM_WAGE, SCALE + 2, ROUNDING)
                    .multiply(HUNDRED).setScale(SCALE, ROUNDING);
        } else {
            realShadowWage = BigDecimal.ZERO;
            minimumWageRatio = BigDecimal.ZERO;
        }

        return ShadowWageDto.builder()
                .totalHours(totalHours.setScale(SCALE, ROUNDING))
                .operatingProfit(operatingProfit.setScale(SCALE, ROUNDING))
                .realShadowWage(realShadowWage)
                .minimumWage(MINIMUM_WAGE)
                .minimumWageRatio(minimumWageRatio)
                .hasTimeData(hasTimeData)
                .build();
    }

    BepDto calculateEnhancedBEP(Long projectId, Project project, BigDecimal costDetailFixedSum) {
        BigDecimal effectiveFixedCost = costDetailFixedSum.compareTo(BigDecimal.ZERO) > 0
                ? costDetailFixedSum : project.getFixedCost();

        BigDecimal contribution = project.getPrice().subtract(project.getVariableCost());
        BigDecimal bep = calculator.calculateBEP(effectiveFixedCost, project.getPrice(),
                project.getVariableCost());

        return BepDto.builder()
                .enhancedFixedCost(effectiveFixedCost.setScale(SCALE, ROUNDING))
                .bep(bep)
                .price(project.getPrice())
                .variableCostPerUnit(project.getVariableCost())
                .contributionMargin(contribution.setScale(SCALE, ROUNDING))
                .build();
    }

    List<ActionCardDto> generateActionCards(Long projectId, Project project,
                                            CostBreakdownDto costBreakdown,
                                            ShadowWageDto shadowWage, BepDto bep) {
        List<ActionCardDto> cards = new ArrayList<>();

        // 실질시급 < 최저임금
        if (shadowWage.getRealShadowWage().compareTo(MINIMUM_WAGE) < 0) {
            cards.add(ActionCardDto.builder()
                    .type("WARNING")
                    .title("시간당 수익이 최저임금 미만입니다")
                    .description(String.format("실질 시급 %s원으로 최저임금 %s원에 미달합니다. 가격 인상 또는 작업 효율 개선을 검토하세요.",
                            shadowWage.getRealShadowWage().toPlainString(),
                            MINIMUM_WAGE.toPlainString()))
                    .priority(1)
                    .build());
        }

        // 경제적 이윤 < 0
        BigDecimal totalHours = shadowWage.getTotalHours();
        BigDecimal economicProfit = calculator.calculateEconomicProfit(
                shadowWage.getOperatingProfit(), totalHours, project.getHourlyWage());
        if (economicProfit.compareTo(BigDecimal.ZERO) < 0) {
            cards.add(ActionCardDto.builder()
                    .type("WARNING")
                    .title("기회비용 고려 시 적자입니다")
                    .description(String.format("경제적 이윤이 %s원입니다. 투입 시간 대비 수익이 기대 시급에 미달합니다.",
                            economicProfit.toPlainString()))
                    .priority(2)
                    .build());
        }

        // 안전마진율
        BigDecimal enhancedFixedCost = bep.getEnhancedFixedCost();
        BigDecimal projectedIncome = project.getHourlyWage()
                .multiply(new BigDecimal(project.getWorkHours()));
        BigDecimal contribution = project.getPrice().subtract(project.getVariableCost());
        BigDecimal targetQuantity = calculator.calculateTargetSales(
                enhancedFixedCost, projectedIncome,
                project.getPrice(), project.getVariableCost());
        BigDecimal marginRate = calculator.calculateMarginRate(targetQuantity, bep.getBep());

        if (marginRate.compareTo(BigDecimal.ZERO) < 0) {
            cards.add(ActionCardDto.builder()
                    .type("WARNING")
                    .title("손익분기점 미달입니다")
                    .description("현재 예상 판매량이 손익분기점에 미달합니다. 비용 절감 또는 가격 조정이 필요합니다.")
                    .priority(1)
                    .build());
        } else if (marginRate.compareTo(new BigDecimal("20")) > 0) {
            cards.add(ActionCardDto.builder()
                    .type("POSITIVE")
                    .title("안정적인 수익 구조입니다")
                    .description(String.format("안전마진율 %s%%로 안정적인 수익 구조를 갖추고 있습니다.",
                            marginRate.toPlainString()))
                    .priority(5)
                    .build());
        }

        // API 비용 비중 30% 초과
        BigDecimal apiRatio = costBreakdown.getCategoryRatio()
                .getOrDefault(CostCategory.API_USAGE, BigDecimal.ZERO);
        if (apiRatio.compareTo(new BigDecimal("30")) > 0) {
            cards.add(ActionCardDto.builder()
                    .type("SUGGESTION")
                    .title("API 비용 최적화를 검토하세요")
                    .description(String.format("API 사용 비용이 전체 비용의 %s%%를 차지합니다. 캐싱, 배치 처리 등으로 최적화를 검토하세요.",
                            apiRatio.toPlainString()))
                    .priority(3)
                    .build());
        }

        // TimeLog 0건
        if (!shadowWage.isHasTimeData()) {
            cards.add(ActionCardDto.builder()
                    .type("SUGGESTION")
                    .title("시간 기록을 시작하면 더 정확한 분석이 가능합니다")
                    .description("작업 시간을 기록하면 실질 시급, 작업 효율성 등 더 정확한 분석 결과를 제공합니다.")
                    .priority(4)
                    .build());
        }

        return cards;
    }

    private BigDecimal getEnhancedFixedCost(Long projectId, Project project) {
        List<CostDetail> fixedCosts = costDetailRepository
                .findByProjectIdAndCostType(projectId, "FIXED");
        BigDecimal sum = fixedCosts.stream()
                .map(CostDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.compareTo(BigDecimal.ZERO) > 0 ? sum : project.getFixedCost();
    }

    private BigDecimal getTotalHoursFromTimeLogs(Long projectId) {
        return timeLogRepository.findByProjectIdOrderByLogDateDesc(projectId).stream()
                .map(TimeLog::getHoursSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", projectId));
    }

    private void validateOwnership(Project project, Long userId) {
        if (!project.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }
    }
}
