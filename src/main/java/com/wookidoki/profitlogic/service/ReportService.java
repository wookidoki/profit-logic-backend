package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.client.LlmClient;
import com.wookidoki.profitlogic.client.LlmResponse;
import com.wookidoki.profitlogic.common.exception.BusinessLogicException;
import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.Report;
import com.wookidoki.profitlogic.dto.*;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String SYSTEM_PROMPT = """
            당신은 1인 크리에이터 전문 경영 컨설턴트입니다.
            아래 데이터를 바탕으로 월간 리포트를 작성하세요.

            리포트 형식:
            📊 핵심 요약 (3줄)
            ⚠️ 위험 신호
            ✅ 실행 액션 3가지
            📈 다음 달 전망

            규칙:
            - 한국어로 작성
            - 전문 용어에는 괄호로 쉬운 설명 추가
            - 구체적인 숫자를 인용하여 근거 제시
            - 실행 가능한 조언 위주 (추상적 조언 금지)""";

    private final ReportRepository reportRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAnalysisService analysisService;
    private final LlmClient llmClient;

    @Transactional
    public ReportResponse generateReport(Long projectId, String yearMonth, Long userId) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);

        // 이미 생성된 리포트가 있으면 반환
        return reportRepository.findByProjectIdAndYearMonth(projectId, yearMonth)
                .map(ReportResponse::from)
                .orElseGet(() -> createNewReport(project, projectId, yearMonth, userId));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getReports(Long projectId, Long userId) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);

        return reportRepository.findByProjectIdOrderByYearMonthDesc(projectId)
                .stream()
                .map(ReportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(Long projectId, Long reportId, Long userId) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("리포트", reportId));

        if (!report.getProject().getId().equals(projectId)) {
            throw new BusinessLogicException("해당 프로젝트의 리포트가 아닙니다.");
        }

        return ReportResponse.from(report);
    }

    private ReportResponse createNewReport(Project project, Long projectId,
                                           String yearMonth, Long userId) {
        if (!llmClient.isAvailable()) {
            throw new BusinessLogicException("리포트 생성을 위해 AI 서비스 설정이 필요합니다.");
        }

        // 분석 데이터 수집
        ProjectAnalysisResponse analysis = analysisService.analyzeProject(projectId, userId);

        String userPrompt = buildUserPrompt(project, yearMonth, analysis);

        LlmResponse llmResponse = llmClient.chatWithUsage(SYSTEM_PROMPT, userPrompt);
        log.info("리포트 생성 완료 - 프로젝트: {}, 월: {}, 토큰: {}",
                project.getTitle(), yearMonth, llmResponse.getTotalTokens());

        Report report = Report.builder()
                .project(project)
                .yearMonth(yearMonth)
                .content(llmResponse.getContent())
                .tokensUsed(llmResponse.getTotalTokens())
                .build();

        return ReportResponse.from(reportRepository.save(report));
    }

    private String buildUserPrompt(Project project, String yearMonth,
                                   ProjectAnalysisResponse analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 프로젝트 정보 ===\n");
        sb.append("프로젝트명: ").append(project.getTitle()).append("\n");
        sb.append("분석 월: ").append(yearMonth).append("\n");

        if (project.getCreatorCategory() != null) {
            sb.append("크리에이터 유형: ").append(project.getCreatorCategory()).append("\n");
        }

        sb.append("\n=== 비용 구조 ===\n");
        CostBreakdownDto cost = analysis.getCostBreakdown();
        sb.append("총 고정비: ").append(cost.getTotalFixedCost().toPlainString()).append("원\n");
        sb.append("총 변동비: ").append(cost.getTotalVariableCost().toPlainString()).append("원\n");
        sb.append("총 비용: ").append(cost.getTotalCost().toPlainString()).append("원\n");

        sb.append("\n=== 실질 시급 ===\n");
        ShadowWageDto wage = analysis.getShadowWage();
        sb.append("총 투입 시간: ").append(wage.getTotalHours().toPlainString()).append("시간\n");
        sb.append("실질 시급: ").append(wage.getRealShadowWage().toPlainString()).append("원\n");
        sb.append("최저임금 대비: ").append(wage.getMinimumWageRatio().toPlainString()).append("%\n");

        sb.append("\n=== 손익분기점 ===\n");
        BepDto bep = analysis.getBep();
        sb.append("BEP: ").append(bep.getBep().toPlainString()).append("개\n");
        sb.append("판매가: ").append(bep.getPrice().toPlainString()).append("원\n");
        sb.append("공헌이익: ").append(bep.getContributionMargin().toPlainString()).append("원\n");

        sb.append("\n=== 분석 진단 ===\n");
        for (ActionCardDto card : analysis.getActionCards()) {
            sb.append("[").append(card.getType()).append("] ").append(card.getTitle())
                    .append(": ").append(card.getDescription()).append("\n");
        }

        return sb.toString();
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
