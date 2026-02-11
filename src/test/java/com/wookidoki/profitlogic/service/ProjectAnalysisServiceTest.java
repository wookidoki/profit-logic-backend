package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.BusinessLogicException;
import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.*;
import com.wookidoki.profitlogic.domain.logic.FinancialCalculator;
import com.wookidoki.profitlogic.dto.*;
import com.wookidoki.profitlogic.repository.CostDetailRepository;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.TimeLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectAnalysisService 단위 테스트")
class ProjectAnalysisServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private CostDetailRepository costDetailRepository;
    @Mock private TimeLogRepository timeLogRepository;
    @Spy private FinancialCalculator calculator;

    @InjectMocks
    private ProjectAnalysisService projectAnalysisService;

    private User createUser(Long id) {
        return User.builder().id(id).email("user@test.com").password("encoded").nickname("테스터").build();
    }

    private Project createProject(Long id, User user) {
        return Project.builder()
                .id(id).user(user).title("이모티콘 프로젝트")
                .price(new BigDecimal("10000"))
                .variableCost(new BigDecimal("2000"))
                .fixedCost(new BigDecimal("500000"))
                .workHours(160)
                .hourlyWage(new BigDecimal("9860"))
                .build();
    }

    private List<CostDetail> createCostDetails(Project project) {
        return List.of(
                CostDetail.builder().id(1L).project(project)
                        .category(CostCategory.SERVER).costName("AWS")
                        .costType("FIXED").amount(new BigDecimal("100000")).build(),
                CostDetail.builder().id(2L).project(project)
                        .category(CostCategory.TOOL_SUBSCRIPTION).costName("Figma")
                        .costType("FIXED").amount(new BigDecimal("15000")).build(),
                CostDetail.builder().id(3L).project(project)
                        .category(CostCategory.API_USAGE).costName("OpenAI")
                        .costType("VARIABLE").amount(new BigDecimal("50000")).build(),
                CostDetail.builder().id(4L).project(project)
                        .category(CostCategory.MATERIAL).costName("재료비")
                        .costType("VARIABLE").amount(new BigDecimal("35000")).build()
        );
    }

    private List<TimeLog> createTimeLogs(Project project) {
        return List.of(
                TimeLog.builder().id(1L).project(project).taskName("스케치")
                        .hoursSpent(new BigDecimal("40")).logDate(LocalDate.of(2026, 1, 10)).build(),
                TimeLog.builder().id(2L).project(project).taskName("채색")
                        .hoursSpent(new BigDecimal("60")).logDate(LocalDate.of(2026, 1, 15)).build(),
                TimeLog.builder().id(3L).project(project).taskName("수정")
                        .hoursSpent(new BigDecimal("100")).logDate(LocalDate.of(2026, 1, 20)).build()
        );
    }

    @Nested
    @DisplayName("종합 분석")
    class AnalyzeProject {

        @Test
        @DisplayName("정상: 프로젝트 종합 분석 결과 반환")
        void analyzeProject_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(costDetailRepository.findByProjectId(10L)).willReturn(createCostDetails(project));
            given(timeLogRepository.findByProjectIdOrderByLogDateDesc(10L))
                    .willReturn(createTimeLogs(project));

            ProjectAnalysisResponse result = projectAnalysisService.analyzeProject(10L, 1L);

            assertThat(result.getProjectId()).isEqualTo(10L);
            assertThat(result.getProjectTitle()).isEqualTo("이모티콘 프로젝트");
            assertThat(result.getCostBreakdown()).isNotNull();
            assertThat(result.getShadowWage()).isNotNull();
            assertThat(result.getBep()).isNotNull();
            assertThat(result.getActionCards()).isNotNull();
        }

        @Test
        @DisplayName("비소유자 접근 → UnauthorizedAccessException")
        void analyzeProject_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            assertThatThrownBy(() -> projectAnalysisService.analyzeProject(10L, 999L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 프로젝트 → ResourceNotFoundException")
        void analyzeProject_notFound_throws() {
            given(projectRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> projectAnalysisService.analyzeProject(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("비용 구조 분석")
    class CostBreakdown {

        @Test
        @DisplayName("CostDetail 있을 때 → FIXED/VARIABLE 합산 및 카테고리 비율")
        void costBreakdown_withDetails() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(costDetailRepository.findByProjectId(10L)).willReturn(createCostDetails(project));

            CostBreakdownDto result = projectAnalysisService.calculateCostBreakdown(10L);

            assertThat(result.getTotalFixedCost()).isEqualByComparingTo(new BigDecimal("115000"));
            assertThat(result.getTotalVariableCost()).isEqualByComparingTo(new BigDecimal("85000"));
            assertThat(result.getTotalCost()).isEqualByComparingTo(new BigDecimal("200000"));
            assertThat(result.getCategoryRatio()).containsKey(CostCategory.SERVER);
            assertThat(result.getCategoryRatio()).containsKey(CostCategory.API_USAGE);
        }

        @Test
        @DisplayName("CostDetail 0건 → 전부 0, 빈 카테고리 맵")
        void costBreakdown_empty() {
            given(costDetailRepository.findByProjectId(10L)).willReturn(List.of());

            CostBreakdownDto result = projectAnalysisService.calculateCostBreakdown(10L);

            assertThat(result.getTotalFixedCost()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalVariableCost()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalCost()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getCategoryRatio()).isEmpty();
        }

        @Test
        @DisplayName("카테고리 비율 합 = 100%")
        void costBreakdown_ratioSum100() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(costDetailRepository.findByProjectId(10L)).willReturn(createCostDetails(project));

            CostBreakdownDto result = projectAnalysisService.calculateCostBreakdown(10L);

            BigDecimal sum = result.getCategoryRatio().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(HUNDRED);
        }

        private static final BigDecimal HUNDRED = new BigDecimal("100");
    }

    @Nested
    @DisplayName("실질 시급 분석")
    class RealShadowWage {

        @Test
        @DisplayName("TimeLog 있을 때 → 실질 시급 계산")
        void shadowWage_withTimeLogs() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(timeLogRepository.findByProjectIdOrderByLogDateDesc(10L))
                    .willReturn(createTimeLogs(project));

            ShadowWageDto result = projectAnalysisService.calculateRealShadowWage(
                    10L, project, project.getFixedCost());

            assertThat(result.isHasTimeData()).isTrue();
            assertThat(result.getTotalHours()).isEqualByComparingTo(new BigDecimal("200"));
            assertThat(result.getRealShadowWage()).isNotNull();
            assertThat(result.getMinimumWage()).isEqualByComparingTo(new BigDecimal("9860"));
        }

        @Test
        @DisplayName("TimeLog 0건 → project.workHours 사용, hasTimeData=false")
        void shadowWage_noTimeLogs() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(timeLogRepository.findByProjectIdOrderByLogDateDesc(10L))
                    .willReturn(List.of());

            ShadowWageDto result = projectAnalysisService.calculateRealShadowWage(
                    10L, project, project.getFixedCost());

            assertThat(result.isHasTimeData()).isFalse();
            assertThat(result.getTotalHours()).isEqualByComparingTo(new BigDecimal("160"));
        }

        @Test
        @DisplayName("실질시급이 최저임금 미만인 경우 비율 < 100%")
        void shadowWage_belowMinimum() {
            User user = createUser(1L);
            Project project = Project.builder()
                    .id(10L).user(user).title("저수익 프로젝트")
                    .price(new BigDecimal("5000")).variableCost(new BigDecimal("2000"))
                    .fixedCost(new BigDecimal("100000")).workHours(100)
                    .hourlyWage(new BigDecimal("5000")).build();

            List<TimeLog> timeLogs = List.of(
                    TimeLog.builder().id(1L).project(project).taskName("작업")
                            .hoursSpent(new BigDecimal("200"))
                            .logDate(LocalDate.of(2026, 1, 10)).build()
            );
            given(timeLogRepository.findByProjectIdOrderByLogDateDesc(10L)).willReturn(timeLogs);

            ShadowWageDto result = projectAnalysisService.calculateRealShadowWage(
                    10L, project, project.getFixedCost());

            assertThat(result.getRealShadowWage()).isLessThan(new BigDecimal("9860"));
            assertThat(result.getMinimumWageRatio()).isLessThan(new BigDecimal("100"));
        }
    }

    @Nested
    @DisplayName("강화 BEP 분석")
    class EnhancedBEP {

        @Test
        @DisplayName("CostDetail 고정비 합산으로 BEP 재계산")
        void enhancedBEP_withCostDetails() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            BigDecimal costDetailFixed = new BigDecimal("115000");

            BepDto result = projectAnalysisService.calculateEnhancedBEP(10L, project, costDetailFixed);

            // BEP = 115000 / (10000 - 2000) = 115000 / 8000 = 14.375 → CEILING → 15
            assertThat(result.getBep()).isEqualByComparingTo(new BigDecimal("15"));
            assertThat(result.getEnhancedFixedCost()).isEqualByComparingTo(new BigDecimal("115000"));
            assertThat(result.getContributionMargin()).isEqualByComparingTo(new BigDecimal("8000"));
        }

        @Test
        @DisplayName("CostDetail 고정비 0 → project.fixedCost 사용")
        void enhancedBEP_fallbackToProject() {
            User user = createUser(1L);
            Project project = createProject(10L, user);

            BepDto result = projectAnalysisService.calculateEnhancedBEP(
                    10L, project, BigDecimal.ZERO);

            // BEP = 500000 / 8000 = 62.5 → CEILING → 63
            assertThat(result.getBep()).isEqualByComparingTo(new BigDecimal("63"));
            assertThat(result.getEnhancedFixedCost()).isEqualByComparingTo(new BigDecimal("500000"));
        }
    }

    @Nested
    @DisplayName("가격 변동 시뮬레이션")
    class PriceSimulation {

        @Test
        @DisplayName("가격 인상 시뮬레이션 → 예상 수익/이익 계산")
        void priceSimulation_increase() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(costDetailRepository.findByProjectIdAndCostType(10L, "FIXED"))
                    .willReturn(List.of());

            PriceSimulationDto result = projectAnalysisService.simulatePriceChange(
                    10L, 1L, new BigDecimal("15000"), new BigDecimal("-1.5"));

            assertThat(result.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(result.getNewPrice()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(result.getPriceChangeRate()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(result.getExpectedDemandChange()).isEqualByComparingTo(new BigDecimal("-75.00"));
        }

        @Test
        @DisplayName("탄력성 미지정 → 기본값 -1.5 사용")
        void priceSimulation_defaultElasticity() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(costDetailRepository.findByProjectIdAndCostType(10L, "FIXED"))
                    .willReturn(List.of());

            PriceSimulationDto result = projectAnalysisService.simulatePriceChange(
                    10L, 1L, new BigDecimal("12000"), null);

            assertThat(result.getElasticity()).isEqualByComparingTo(new BigDecimal("-1.5"));
        }

        @Test
        @DisplayName("판매가 0원 → BusinessLogicException")
        void priceSimulation_zeroPriceThrows() {
            User user = createUser(1L);
            Project project = Project.builder()
                    .id(10L).user(user).title("무료 프로젝트")
                    .price(BigDecimal.ZERO).variableCost(BigDecimal.ZERO)
                    .fixedCost(new BigDecimal("500000")).workHours(160)
                    .hourlyWage(new BigDecimal("9860")).build();
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            assertThatThrownBy(() -> projectAnalysisService.simulatePriceChange(
                    10L, 1L, new BigDecimal("5000"), null))
                    .isInstanceOf(BusinessLogicException.class)
                    .hasMessageContaining("판매가가 0원");
        }

        @Test
        @DisplayName("비소유자 → UnauthorizedAccessException")
        void priceSimulation_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            assertThatThrownBy(() -> projectAnalysisService.simulatePriceChange(
                    10L, 999L, new BigDecimal("15000"), null))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    @Nested
    @DisplayName("액션 카드 생성")
    class ActionCards {

        @Test
        @DisplayName("실질시급 < 최저임금 → WARNING 카드 생성")
        void actionCard_lowShadowWage() {
            User user = createUser(1L);
            Project project = Project.builder()
                    .id(10L).user(user).title("프로젝트")
                    .price(new BigDecimal("10000")).variableCost(new BigDecimal("2000"))
                    .fixedCost(new BigDecimal("500000")).workHours(100)
                    .hourlyWage(new BigDecimal("5000")).build();

            ShadowWageDto shadowWage = ShadowWageDto.builder()
                    .totalHours(new BigDecimal("200"))
                    .operatingProfit(new BigDecimal("500000"))
                    .realShadowWage(new BigDecimal("2500"))
                    .minimumWage(new BigDecimal("9860"))
                    .minimumWageRatio(new BigDecimal("25.35"))
                    .hasTimeData(true).build();

            BepDto bep = BepDto.builder()
                    .enhancedFixedCost(new BigDecimal("500000"))
                    .bep(new BigDecimal("63"))
                    .price(new BigDecimal("10000"))
                    .variableCostPerUnit(new BigDecimal("2000"))
                    .contributionMargin(new BigDecimal("8000")).build();

            CostBreakdownDto costBreakdown = CostBreakdownDto.builder()
                    .totalFixedCost(new BigDecimal("500000"))
                    .totalVariableCost(BigDecimal.ZERO)
                    .totalCost(new BigDecimal("500000"))
                    .categoryRatio(java.util.Map.of()).build();

            List<ActionCardDto> cards = projectAnalysisService.generateActionCards(
                    10L, project, costBreakdown, shadowWage, bep);

            assertThat(cards).anyMatch(c -> "WARNING".equals(c.getType())
                    && c.getTitle().contains("최저임금"));
        }

        @Test
        @DisplayName("TimeLog 0건 → SUGGESTION 카드 생성")
        void actionCard_noTimeLogs() {
            User user = createUser(1L);
            Project project = createProject(10L, user);

            ShadowWageDto shadowWage = ShadowWageDto.builder()
                    .totalHours(new BigDecimal("160"))
                    .operatingProfit(new BigDecimal("1577600"))
                    .realShadowWage(new BigDecimal("9860"))
                    .minimumWage(new BigDecimal("9860"))
                    .minimumWageRatio(new BigDecimal("100"))
                    .hasTimeData(false).build();

            BepDto bep = BepDto.builder()
                    .enhancedFixedCost(new BigDecimal("500000"))
                    .bep(new BigDecimal("63"))
                    .price(new BigDecimal("10000"))
                    .variableCostPerUnit(new BigDecimal("2000"))
                    .contributionMargin(new BigDecimal("8000")).build();

            CostBreakdownDto costBreakdown = CostBreakdownDto.builder()
                    .totalFixedCost(new BigDecimal("500000"))
                    .totalVariableCost(BigDecimal.ZERO)
                    .totalCost(new BigDecimal("500000"))
                    .categoryRatio(java.util.Map.of()).build();

            List<ActionCardDto> cards = projectAnalysisService.generateActionCards(
                    10L, project, costBreakdown, shadowWage, bep);

            assertThat(cards).anyMatch(c -> "SUGGESTION".equals(c.getType())
                    && c.getTitle().contains("시간 기록"));
        }

        @Test
        @DisplayName("API 비용 > 30% → SUGGESTION 카드 생성")
        void actionCard_highApiCost() {
            User user = createUser(1L);
            Project project = createProject(10L, user);

            ShadowWageDto shadowWage = ShadowWageDto.builder()
                    .totalHours(new BigDecimal("160"))
                    .operatingProfit(new BigDecimal("1577600"))
                    .realShadowWage(new BigDecimal("9860"))
                    .minimumWage(new BigDecimal("9860"))
                    .minimumWageRatio(new BigDecimal("100"))
                    .hasTimeData(true).build();

            BepDto bep = BepDto.builder()
                    .enhancedFixedCost(new BigDecimal("500000"))
                    .bep(new BigDecimal("63"))
                    .price(new BigDecimal("10000"))
                    .variableCostPerUnit(new BigDecimal("2000"))
                    .contributionMargin(new BigDecimal("8000")).build();

            CostBreakdownDto costBreakdown = CostBreakdownDto.builder()
                    .totalFixedCost(new BigDecimal("200000"))
                    .totalVariableCost(new BigDecimal("300000"))
                    .totalCost(new BigDecimal("500000"))
                    .categoryRatio(java.util.Map.of(CostCategory.API_USAGE, new BigDecimal("40.00")))
                    .build();

            List<ActionCardDto> cards = projectAnalysisService.generateActionCards(
                    10L, project, costBreakdown, shadowWage, bep);

            assertThat(cards).anyMatch(c -> "SUGGESTION".equals(c.getType())
                    && c.getTitle().contains("API"));
        }

        @Test
        @DisplayName("안전마진율 > 20% → POSITIVE 카드 생성")
        void actionCard_highMargin() {
            User user = createUser(1L);
            Project project = Project.builder()
                    .id(10L).user(user).title("고수익 프로젝트")
                    .price(new BigDecimal("50000")).variableCost(new BigDecimal("5000"))
                    .fixedCost(new BigDecimal("100000")).workHours(50)
                    .hourlyWage(new BigDecimal("20000")).build();

            ShadowWageDto shadowWage = ShadowWageDto.builder()
                    .totalHours(new BigDecimal("50"))
                    .operatingProfit(new BigDecimal("1000000"))
                    .realShadowWage(new BigDecimal("20000"))
                    .minimumWage(new BigDecimal("9860"))
                    .minimumWageRatio(new BigDecimal("202.84"))
                    .hasTimeData(true).build();

            BepDto bep = BepDto.builder()
                    .enhancedFixedCost(new BigDecimal("100000"))
                    .bep(new BigDecimal("3"))
                    .price(new BigDecimal("50000"))
                    .variableCostPerUnit(new BigDecimal("5000"))
                    .contributionMargin(new BigDecimal("45000")).build();

            CostBreakdownDto costBreakdown = CostBreakdownDto.builder()
                    .totalFixedCost(new BigDecimal("100000"))
                    .totalVariableCost(BigDecimal.ZERO)
                    .totalCost(new BigDecimal("100000"))
                    .categoryRatio(java.util.Map.of()).build();

            List<ActionCardDto> cards = projectAnalysisService.generateActionCards(
                    10L, project, costBreakdown, shadowWage, bep);

            assertThat(cards).anyMatch(c -> "POSITIVE".equals(c.getType())
                    && c.getTitle().contains("안정적"));
        }
    }
}
