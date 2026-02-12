package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.client.LlmClient;
import com.wookidoki.profitlogic.client.LlmResponse;
import com.wookidoki.profitlogic.client.PromptTemplates;
import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.ChatLog;
import com.wookidoki.profitlogic.domain.CreatorCategory;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.chat.ChatRequest;
import com.wookidoki.profitlogic.dto.chat.ChatResponse;
import com.wookidoki.profitlogic.dto.finance.ActionCardDto;
import com.wookidoki.profitlogic.dto.project.ProjectAnalysisResponse;
import com.wookidoki.profitlogic.repository.ChatLogRepository;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatLogRepository chatLogRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final LlmClient llmClient;
    private final ProjectAnalysisService projectAnalysisService;

    @Transactional
    public ChatResponse chat(Long userId, ChatRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자", userId));
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", request.getProjectId()));

        if (!project.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }

        String answer;
        int tokensUsed = 0;

        if (llmClient.isAvailable()) {
            String systemPrompt = buildSystemPrompt(project, userId);
            LlmResponse llmResponse = llmClient.chatWithUsage(systemPrompt, request.getQuestion());
            answer = llmResponse.getContent();
            tokensUsed = llmResponse.getTotalTokens();
            log.debug("LLM 응답 완료 - 프로젝트: {}, 토큰: {}", project.getTitle(), tokensUsed);
        } else {
            answer = generateRuleBasedAnswer(request.getQuestion(), project);
            log.debug("규칙 기반 응답 - LLM 미설정");
        }

        ChatLog chatLog = ChatLog.builder()
                .user(user)
                .project(project)
                .question(request.getQuestion())
                .answer(answer)
                .tokensUsed(tokensUsed)
                .build();

        return ChatResponse.from(chatLogRepository.save(chatLog));
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getHistory(Long userId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", projectId));

        if (!project.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }

        return chatLogRepository.findByUserIdAndProjectIdOrderByCreatedAtDesc(userId, projectId)
                .stream()
                .map(ChatResponse::from)
                .toList();
    }

    String buildSystemPrompt(Project project, Long userId) {
        BigDecimal cm = project.getPrice().subtract(project.getVariableCost());
        double dailyHours = project.getWorkHours() / 22.0;

        StringBuilder sb = new StringBuilder();
        sb.append(PromptTemplates.FINANCIAL_ADVISOR);
        sb.append("\n\n");

        // === 사이드 프로젝트 정보 ===
        sb.append("=== 사이드 프로젝트 정보 ===\n");
        sb.append("프로젝트명: ").append(project.getTitle()).append("\n");
        if (project.getCreatorCategory() != null) {
            sb.append("크리에이터 유형: ").append(project.getCreatorCategory().getDisplayName()).append("\n");
        }
        sb.append("건당 수익: ").append(project.getPrice().toPlainString()).append("원\n");
        sb.append("건당 비용: ").append(project.getVariableCost().toPlainString()).append("원\n");
        sb.append("건당 순수익: ").append(cm.toPlainString()).append("원\n");
        sb.append("월 고정 지출: ").append(project.getFixedCost().toPlainString()).append("원\n");
        sb.append("월 투입 시간: ").append(project.getWorkHours())
                .append("시간 (하루 약 ").append(String.format("%.1f", dailyHours)).append("시간)\n");
        sb.append("본업 시급(기회비용): ").append(project.getHourlyWage().toPlainString()).append("원\n");

        // === Profit Logic 엔진 분석 결과 ===
        try {
            ProjectAnalysisResponse analysis = projectAnalysisService.analyzeProject(
                    project.getId(), userId);

            sb.append("\n=== Profit Logic 엔진 분석 결과 ===\n");

            if (analysis.getBep() != null) {
                sb.append("월 최소 작업량(BEP): ").append(analysis.getBep().getBep().toPlainString()).append("건\n");
            }

            if (analysis.getShadowWage() != null) {
                BigDecimal realWage = analysis.getShadowWage().getRealShadowWage();
                sb.append("실질 시급: ").append(realWage.toPlainString()).append("원\n");

                // 본업 시급 대비 비율
                if (project.getHourlyWage().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal wageRatio = realWage.multiply(HUNDRED)
                            .divide(project.getHourlyWage(), 1, RoundingMode.HALF_UP);
                    sb.append("본업 시급 대비: ").append(wageRatio.toPlainString()).append("%\n");
                }

                sb.append("최저임금 대비: ").append(analysis.getShadowWage().getMinimumWageRatio().toPlainString()).append("%\n");
                sb.append("실제 시간 기록: ").append(analysis.getShadowWage().isHasTimeData() ? "있음" : "없음 (설정값 기준 추정)").append("\n");
            }

            if (analysis.getCostBreakdown() != null
                    && analysis.getCostBreakdown().getTotalCost().compareTo(BigDecimal.ZERO) > 0) {
                sb.append("비용 구조 요약: 고정 ").append(analysis.getCostBreakdown().getTotalFixedCost().toPlainString())
                        .append("원 + 건당 ").append(analysis.getCostBreakdown().getTotalVariableCost().toPlainString()).append("원\n");
            }

            if (analysis.getActionCards() != null && !analysis.getActionCards().isEmpty()) {
                sb.append("\n=== 주요 인사이트 ===\n");
                for (ActionCardDto card : analysis.getActionCards()) {
                    sb.append("- [").append(card.getType()).append("] ").append(card.getTitle())
                            .append(": ").append(card.getDescription()).append("\n");
                }
            }
        } catch (Exception e) {
            log.debug("분석 데이터 로드 실패, 기본 프로젝트 정보만 사용: {}", e.getMessage());
        }

        // === 목표 정보 ===
        if (project.getTargetRevenue() != null && project.getTargetRevenue().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("\n=== 목표 정보 ===\n");
            sb.append("목표 월 수익: ").append(project.getTargetRevenue().toPlainString()).append("원\n");
            if (project.getTargetMonth() != null) {
                sb.append("목표 시점: ").append(project.getTargetMonth()).append("\n");
            }
        }

        // === 크리에이터 유형별 맥락 ===
        if (project.getCreatorCategory() != null) {
            sb.append("\n=== 이 유형의 특성 (참고 맥락) ===\n");
            sb.append(getCategoryContext(project.getCreatorCategory()));
        }

        return sb.toString();
    }

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private String getCategoryContext(CreatorCategory category) {
        return switch (category) {
            case WEB_NOVEL -> """
                    - 회차 단위 수익, 연재 주기가 수입 주기
                    - 유료 전환율과 독자 유지율이 실질 시급 결정
                    - 작업 속도(시간당 글자 수) 개선이 시급 향상의 핵심
                    """;
            case SHORT_FORM -> """
                    - 조회수 기반 수익, 건당 수익 변동 큼
                    - 촬영+편집 시간 대비 수익 효율이 핵심
                    - 협찬/브랜디드가 광고보다 단가 높은 경우 많음
                    """;
            case EMOTICON -> """
                    - 세트 단위, 승인되면 스톡형 수동 수익
                    - 제작 시간 집중 후 장기 회수 모델
                    - 승인률이 시간 효율에 큰 영향
                    """;
            case BLOG -> """
                    - 포스팅 누적 → 트래픽 → 수익의 복리 구조
                    - 초기 수익 낮지만 장기적 시간당 효율 상승
                    - SEO 투자가 6개월~1년 후 수익으로 전환
                    """;
            case INDIE_DEV -> """
                    - 초기 개발은 수익 0, 시간 투자만 존재
                    - 런칭 후 MRR이 핵심 지표
                    - 본업 시급 대비 "언제 BEP 도달하나"가 판단 기준
                    """;
        };
    }

    private String generateRuleBasedAnswer(String question, Project project) {
        String q = question.toLowerCase();
        BigDecimal cm = project.getPrice().subtract(project.getVariableCost());
        boolean hasCm = cm.compareTo(BigDecimal.ZERO) > 0;

        // BEP / 최소 건수
        if (q.contains("손익분기") || q.contains("bep") || q.contains("몇 건") || q.contains("몇건")
                || q.contains("몇 개") || q.contains("몇개") || q.contains("최소") || q.contains("본전")) {
            if (!hasCm) {
                return String.format("프로젝트 '%s'의 공헌이익이 0 이하입니다. " +
                        "건당 수익(%s원)이 건당 비용(%s원)보다 높아야 월 최소 건수를 계산할 수 있습니다. " +
                        "수익을 올리거나 건당 비용을 줄이는 것을 추천합니다.",
                        project.getTitle(), project.getPrice(), project.getVariableCost());
            }
            BigDecimal bep = project.getFixedCost()
                    .divide(cm, 1, RoundingMode.HALF_UP);
            return String.format("프로젝트 '%s'의 월 최소 건수(BEP)는 약 %s건입니다.\n\n" +
                    "계산 근거:\n" +
                    "- 건당 수익: %s원\n" +
                    "- 건당 비용: %s원\n" +
                    "- 공헌이익: %s원\n" +
                    "- 월 고정 지출: %s원\n" +
                    "- BEP = 월 고정 지출 ÷ 공헌이익 = %s ÷ %s = %s건\n\n" +
                    "월 %s건 이상 달성하면 고정 지출을 넘어 이익이 발생합니다.",
                    project.getTitle(), bep,
                    project.getPrice(), project.getVariableCost(), cm,
                    project.getFixedCost(), project.getFixedCost(), cm, bep, bep);
        }

        // 실질 시급
        if (q.contains("시급") || q.contains("급여") || q.contains("임금") || q.contains("shadow")) {
            BigDecimal laborCost = project.getHourlyWage()
                    .multiply(BigDecimal.valueOf(project.getWorkHours()));
            return String.format("프로젝트 '%s'의 시간 가치 분석입니다.\n\n" +
                    "- 본업 시급: %s원/시간\n" +
                    "- 월 투입 시간: %d시간\n" +
                    "- 같은 시간 본업 수익(기회비용): %s원\n\n" +
                    "이 사이드 프로젝트의 실질 시급이 본업 시급(%s원)보다 낮다면, " +
                    "시간 투자 대비 가치를 재검토할 필요가 있습니다.\n" +
                    "대시보드 '분석 결과' 탭에서 정확한 실질 시급을 확인하세요.",
                    project.getTitle(), project.getHourlyWage(),
                    project.getWorkHours(), laborCost, project.getHourlyWage());
        }

        // 수익성/이익
        if (q.contains("수익") || q.contains("이익") || q.contains("매출") || q.contains("돈")) {
            BigDecimal cmRate = hasCm
                    ? cm.multiply(BigDecimal.valueOf(100))
                        .divide(project.getPrice(), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return String.format("프로젝트 '%s'의 수익 구조입니다.\n\n" +
                    "- 건당 수익: %s원\n" +
                    "- 건당 비용: %s원\n" +
                    "- 공헌이익: %s원 (공헌이익률 %s%%)\n" +
                    "- 월 고정 지출: %s원\n\n" +
                    "%s",
                    project.getTitle(), project.getPrice(), project.getVariableCost(),
                    cm, cmRate, project.getFixedCost(),
                    cmRate.compareTo(BigDecimal.valueOf(50)) > 0
                        ? "공헌이익률이 높아서 좋은 수익 구조입니다!"
                        : cmRate.compareTo(BigDecimal.valueOf(20)) < 0
                            ? "공헌이익률이 낮습니다. 건당 수익을 올리거나 건당 비용을 줄여보세요."
                            : "보통 수준의 공헌이익률입니다. 건수를 늘리면 수익이 개선됩니다.");
        }

        // 비용 관련
        if (q.contains("비용") || q.contains("절감") || q.contains("줄") || q.contains("아끼") || q.contains("지출")) {
            return String.format("프로젝트 '%s'의 비용 구조를 분석해보겠습니다.\n\n" +
                    "- 월 고정 지출: %s원 (매달 나가는 비용)\n" +
                    "- 건당 비용: %s원 (건마다 드는 비용)\n\n" +
                    "비용 절감 팁:\n" +
                    "1. 고정 지출 절감: 구독 서비스 정리, 대안 도구 검토\n" +
                    "2. 건당 비용 절감: 작업 효율화, 자동화 도입\n" +
                    "3. '비용 상세' 탭에서 항목별 비용을 기록하면 더 정확한 분석이 가능합니다.",
                    project.getTitle(), project.getFixedCost(), project.getVariableCost());
        }

        // 가격 관련
        if (q.contains("가격") || q.contains("올려") || q.contains("인상") || q.contains("할인")) {
            if (!hasCm) {
                return "현재 건당 수익이 건당 비용보다 낮습니다. 수익 단가를 올리는 것이 필수입니다.";
            }
            BigDecimal currentBep = project.getFixedCost()
                    .divide(cm, 1, RoundingMode.HALF_UP);
            BigDecimal newPrice = project.getPrice()
                    .multiply(BigDecimal.valueOf(1.1));
            BigDecimal newCm = newPrice.subtract(project.getVariableCost());
            BigDecimal newBep = project.getFixedCost()
                    .divide(newCm, 1, RoundingMode.HALF_UP);
            return String.format("건당 수익 시뮬레이션 결과입니다.\n\n" +
                    "현재: 건당 수익 %s원 → 월 최소 %s건\n" +
                    "10%% 인상 시: 건당 수익 %s원 → 월 최소 %s건\n\n" +
                    "건당 수익을 10%% 올리면 월 최소 건수가 %s건 줄어들어 더 빨리 이익이 발생합니다.\n" +
                    "'시뮬레이션' 탭에서 다양한 수익 단가를 테스트해보세요.",
                    project.getPrice(), currentBep,
                    newPrice.setScale(0, RoundingMode.HALF_UP), newBep,
                    currentBep.subtract(newBep));
        }

        // 시간 관련
        if (q.contains("시간") || q.contains("작업") || q.contains("효율") || q.contains("투입")) {
            return String.format("프로젝트 '%s'의 시간 분석입니다.\n\n" +
                    "- 월 %d시간 투입 중\n" +
                    "- 하루 약 %.1f시간 (22일 기준)\n\n" +
                    "'작업시간' 탭에서 일별 작업시간을 기록하면 " +
                    "실제 투입 시간 대비 수익을 더 정확하게 분석할 수 있습니다.",
                    project.getTitle(), project.getWorkHours(),
                    project.getWorkHours() / 22.0);
        }

        // 전략/방법/어떻게 관련
        if (q.contains("전략") || q.contains("방법") || q.contains("어떻게") || q.contains("조언") || q.contains("추천")) {
            BigDecimal cmRate = hasCm
                    ? cm.multiply(BigDecimal.valueOf(100))
                        .divide(project.getPrice(), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            StringBuilder sb2 = new StringBuilder();
            sb2.append(String.format("프로젝트 '%s' 개선 전략을 제안해드립니다.\n\n", project.getTitle()));

            if (!hasCm) {
                sb2.append("1. [긴급] 현재 건당 수익이 건당 비용보다 낮습니다. 수익 단가 인상이 최우선입니다.\n");
            } else if (cmRate.compareTo(BigDecimal.valueOf(30)) < 0) {
                sb2.append("1. 공헌이익률이 낮습니다. 건당 비용 절감 또는 수익 단가 인상을 고려하세요.\n");
            } else {
                sb2.append("1. 공헌이익률이 양호합니다. 건수 확대에 집중하세요.\n");
            }

            if (project.getFixedCost().compareTo(BigDecimal.valueOf(500000)) > 0) {
                sb2.append("2. 월 고정 지출이 높은 편입니다. 불필요한 구독/렌탈 비용을 점검하세요.\n");
            } else {
                sb2.append("2. 월 고정 지출은 적정 수준입니다.\n");
            }

            sb2.append("3. 시뮬레이션 탭에서 수익/비용 변경 효과를 미리 확인해보세요.\n");
            sb2.append("4. 대시보드 트렌드 탭에서 월별 추이를 확인하세요.");
            return sb2.toString();
        }

        // 비교 관련
        if (q.contains("비교") || q.contains("경쟁") || q.contains("시장") || q.contains("평균")) {
            return String.format("프로젝트 '%s'의 현재 지표 요약입니다.\n\n" +
                    "- 건당 수익: %s원\n" +
                    "- 공헌이익: %s원\n" +
                    "- 공헌이익률: %s%%\n\n" +
                    "일반적으로 공헌이익률 30%% 이상이면 건전한 수익 구조입니다.\n" +
                    "시뮬레이션 탭에서 다양한 시나리오를 테스트해보세요.",
                    project.getTitle(), project.getPrice(), cm,
                    hasCm ? cm.multiply(BigDecimal.valueOf(100))
                            .divide(project.getPrice(), 1, RoundingMode.HALF_UP) : "0");
        }

        // 목표/달성 관련
        if (q.contains("목표") || q.contains("달성") || q.contains("얼마나") || q.contains("필요")) {
            if (!hasCm) {
                return "현재 공헌이익이 0 이하이므로 목표 달성이 불가능합니다. 건당 수익을 먼저 높여보세요.";
            }
            BigDecimal laborCost = project.getHourlyWage()
                    .multiply(BigDecimal.valueOf(project.getWorkHours()));
            BigDecimal totalNeeded = project.getFixedCost().add(laborCost);
            BigDecimal neededQty = totalNeeded
                    .divide(cm, 1, RoundingMode.HALF_UP);
            return String.format("프로젝트 '%s'의 목표 분석입니다.\n\n" +
                    "- 월 고정 지출: %s원\n" +
                    "- 같은 시간 본업 수익(기회비용): %s원\n" +
                    "- 합계 필요 금액: %s원\n" +
                    "- 공헌이익: %s원/건\n\n" +
                    "→ 기회비용 포함 월 최소 건수: 약 %s건/월\n" +
                    "이 이상 달성해야 본업 시급(%s원) 이상의 실질 수익이 발생합니다.",
                    project.getTitle(), project.getFixedCost(), laborCost,
                    totalNeeded, cm, neededQty, project.getHourlyWage());
        }

        // 요약/종합
        if (q.contains("요약") || q.contains("종합") || q.contains("전체") || q.contains("현황") || q.contains("상태")) {
            BigDecimal bepVal = hasCm
                    ? project.getFixedCost().divide(cm, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal cmRate = hasCm
                    ? cm.multiply(BigDecimal.valueOf(100))
                        .divide(project.getPrice(), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return String.format("프로젝트 '%s' 종합 요약입니다.\n\n" +
                    "핵심 지표\n" +
                    "- 건당 수익: %s원 | 건당 비용: %s원\n" +
                    "- 공헌이익: %s원 (공헌이익률 %s%%)\n" +
                    "- 월 고정 지출: %s원\n" +
                    "- 월 최소 건수(BEP): %s건\n" +
                    "- 본업 시급: %s원 | 월 투입 시간: %d시간\n\n" +
                    "%s",
                    project.getTitle(),
                    project.getPrice(), project.getVariableCost(),
                    cm, cmRate, project.getFixedCost(), bepVal,
                    project.getHourlyWage(), project.getWorkHours(),
                    hasCm ? "더 자세한 분석은 대시보드에서 확인하세요." : "공헌이익이 0 이하입니다. 건당 수익 조정이 필요합니다.");
        }

        return String.format("'%s' 프로젝트에 대해 궁금하신 점이 있으시군요.\n\n" +
                "이런 질문을 해보세요:\n" +
                "- \"월 최소 몇 건을 해야 본전이야?\"\n" +
                "- \"실질 시급은 얼마야?\"\n" +
                "- \"비용을 줄일 방법이 있을까?\"\n" +
                "- \"건당 수익을 올려도 될까?\"\n" +
                "- \"수익 구조는 어때?\"\n" +
                "- \"전체 현황 요약해줘\"\n" +
                "- \"목표 달성하려면 몇 건 해야 해?\"\n" +
                "- \"개선 전략을 추천해줘\"",
                project.getTitle());
    }
}
