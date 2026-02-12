package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.client.LlmClient;
import com.wookidoki.profitlogic.client.LlmResponse;
import com.wookidoki.profitlogic.client.PromptTemplates;
import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.ChatLog;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.chat.ChatRequest;
import com.wookidoki.profitlogic.dto.chat.ChatResponse;
import com.wookidoki.profitlogic.repository.ChatLogRepository;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatLogRepository chatLogRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final LlmClient llmClient;

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
            String systemPrompt = buildSystemPrompt(project);
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

    String buildSystemPrompt(Project project) {
        return PromptTemplates.FINANCIAL_ADVISOR + "\n\n" +
                "=== 현재 프로젝트 정보 ===\n" +
                "프로젝트명: " + project.getTitle() + "\n" +
                "판매가: " + project.getPrice().toPlainString() + "원\n" +
                "변동비: " + project.getVariableCost().toPlainString() + "원\n" +
                "고정비: " + project.getFixedCost().toPlainString() + "원\n" +
                "공헌이익: " + project.getPrice().subtract(project.getVariableCost()).toPlainString() + "원\n" +
                "근무시간: " + project.getWorkHours() + "시간\n" +
                "시급: " + project.getHourlyWage().toPlainString() + "원";
    }

    private String generateRuleBasedAnswer(String question, Project project) {
        String q = question.toLowerCase();
        java.math.BigDecimal cm = project.getPrice().subtract(project.getVariableCost());
        boolean hasCm = cm.compareTo(java.math.BigDecimal.ZERO) > 0;

        // BEP 계산
        if (q.contains("손익분기") || q.contains("bep") || q.contains("몇 개") || q.contains("몇개")) {
            if (!hasCm) {
                return String.format("프로젝트 '%s'의 공헌이익이 0 이하입니다. " +
                        "판매가(%s원)가 변동비(%s원)보다 높아야 손익분기점을 계산할 수 있습니다. " +
                        "가격을 올리거나 변동비를 줄이는 것을 추천합니다.",
                        project.getTitle(), project.getPrice(), project.getVariableCost());
            }
            java.math.BigDecimal bep = project.getFixedCost()
                    .divide(cm, 1, java.math.RoundingMode.HALF_UP);
            return String.format("프로젝트 '%s'의 손익분기점(BEP)은 약 %s개입니다.\n\n" +
                    "계산 근거:\n" +
                    "- 판매가: %s원\n" +
                    "- 변동비: %s원\n" +
                    "- 공헌이익: %s원\n" +
                    "- 고정비: %s원\n" +
                    "- BEP = 고정비 ÷ 공헌이익 = %s ÷ %s = %s개\n\n" +
                    "월 %s개 이상 판매하면 이익이 발생합니다.",
                    project.getTitle(), bep,
                    project.getPrice(), project.getVariableCost(), cm,
                    project.getFixedCost(), project.getFixedCost(), cm, bep, bep);
        }

        // 실질 시급
        if (q.contains("시급") || q.contains("급여") || q.contains("임금") || q.contains("shadow")) {
            java.math.BigDecimal laborCost = project.getHourlyWage()
                    .multiply(java.math.BigDecimal.valueOf(project.getWorkHours()));
            return String.format("프로젝트 '%s'의 시급 분석입니다.\n\n" +
                    "- 설정 시급: %s원/시간\n" +
                    "- 월 근무시간: %d시간\n" +
                    "- 월 인건비(기회비용): %s원\n\n" +
                    "실질 시급은 실제 영업이익을 근무시간으로 나눈 값입니다. " +
                    "대시보드 '종합 분석' 탭에서 정확한 실질 시급을 확인하실 수 있습니다.",
                    project.getTitle(), project.getHourlyWage(),
                    project.getWorkHours(), laborCost);
        }

        // 수익성/이익
        if (q.contains("수익") || q.contains("이익") || q.contains("매출") || q.contains("돈")) {
            java.math.BigDecimal cmRate = hasCm
                    ? cm.multiply(java.math.BigDecimal.valueOf(100))
                        .divide(project.getPrice(), 1, java.math.RoundingMode.HALF_UP)
                    : java.math.BigDecimal.ZERO;
            return String.format("프로젝트 '%s'의 수익 구조입니다.\n\n" +
                    "- 판매가: %s원\n" +
                    "- 변동비: %s원 (개당 원가)\n" +
                    "- 공헌이익: %s원 (공헌이익률 %s%%)\n" +
                    "- 월 고정비: %s원\n\n" +
                    "%s",
                    project.getTitle(), project.getPrice(), project.getVariableCost(),
                    cm, cmRate, project.getFixedCost(),
                    cmRate.compareTo(java.math.BigDecimal.valueOf(50)) > 0
                        ? "공헌이익률이 높아서 좋은 수익 구조입니다!"
                        : cmRate.compareTo(java.math.BigDecimal.valueOf(20)) < 0
                            ? "공헌이익률이 낮습니다. 가격 인상이나 변동비 절감을 검토해보세요."
                            : "보통 수준의 공헌이익률입니다. 판매량을 늘리면 수익이 개선됩니다.");
        }

        // 비용 관련
        if (q.contains("비용") || q.contains("절감") || q.contains("줄") || q.contains("아끼")) {
            return String.format("프로젝트 '%s'의 비용 구조를 분석해보겠습니다.\n\n" +
                    "- 고정비: %s원/월 (매달 나가는 비용)\n" +
                    "- 변동비: %s원/개 (판매할 때마다 드는 비용)\n\n" +
                    "비용 절감 팁:\n" +
                    "1. 고정비 절감: 구독 서비스 정리, 대안 도구 검토\n" +
                    "2. 변동비 절감: 재료 대량 구매, 공정 효율화\n" +
                    "3. '비용 상세' 탭에서 항목별 비용을 기록하면 더 정확한 분석이 가능합니다.",
                    project.getTitle(), project.getFixedCost(), project.getVariableCost());
        }

        // 가격 관련
        if (q.contains("가격") || q.contains("올려") || q.contains("인상") || q.contains("할인")) {
            if (!hasCm) {
                return "현재 판매가가 변동비보다 낮습니다. 가격 인상이 필수입니다.";
            }
            java.math.BigDecimal currentBep = project.getFixedCost()
                    .divide(cm, 1, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal newPrice = project.getPrice()
                    .multiply(java.math.BigDecimal.valueOf(1.1));
            java.math.BigDecimal newCm = newPrice.subtract(project.getVariableCost());
            java.math.BigDecimal newBep = project.getFixedCost()
                    .divide(newCm, 1, java.math.RoundingMode.HALF_UP);
            return String.format("가격 시뮬레이션 결과입니다.\n\n" +
                    "현재: 판매가 %s원 → BEP %s개\n" +
                    "10%% 인상 시: 판매가 %s원 → BEP %s개\n\n" +
                    "가격을 10%% 올리면 BEP가 %s개 줄어들어 더 빨리 이익이 발생합니다.\n" +
                    "'시나리오 시뮬레이션' 기능에서 다양한 가격을 테스트해보세요.",
                    project.getPrice(), currentBep,
                    newPrice.setScale(0, java.math.RoundingMode.HALF_UP), newBep,
                    currentBep.subtract(newBep));
        }

        // 시간 관련
        if (q.contains("시간") || q.contains("작업") || q.contains("효율")) {
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
            java.math.BigDecimal cmRate = hasCm
                    ? cm.multiply(java.math.BigDecimal.valueOf(100))
                        .divide(project.getPrice(), 1, java.math.RoundingMode.HALF_UP)
                    : java.math.BigDecimal.ZERO;
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("프로젝트 '%s' 개선 전략을 제안해드립니다.\n\n", project.getTitle()));

            if (!hasCm) {
                sb.append("1. [긴급] 현재 판매가가 변동비보다 낮습니다. 가격 인상이 최우선입니다.\n");
            } else if (cmRate.compareTo(java.math.BigDecimal.valueOf(30)) < 0) {
                sb.append("1. 공헌이익률이 낮습니다. 변동비 절감 또는 가격 인상을 고려하세요.\n");
            } else {
                sb.append("1. 공헌이익률이 양호합니다. 판매량 확대에 집중하세요.\n");
            }

            if (project.getFixedCost().compareTo(java.math.BigDecimal.valueOf(500000)) > 0) {
                sb.append("2. 고정비가 높은 편입니다. 불필요한 구독/렌탈 비용을 점검하세요.\n");
            } else {
                sb.append("2. 고정비는 적정 수준입니다.\n");
            }

            sb.append("3. 시나리오 시뮬레이션에서 가격/비용 변경 효과를 미리 확인해보세요.\n");
            sb.append("4. 대시보드 트렌드 탭에서 월별 추이를 확인하세요.");
            return sb.toString();
        }

        // 비교 관련
        if (q.contains("비교") || q.contains("경쟁") || q.contains("시장") || q.contains("평균")) {
            return String.format("프로젝트 '%s'의 현재 지표 요약입니다.\n\n" +
                    "- 판매가: %s원\n" +
                    "- 공헌이익: %s원\n" +
                    "- 공헌이익률: %s%%\n\n" +
                    "일반적으로 공헌이익률 30%% 이상이면 건전한 수익 구조입니다.\n" +
                    "시나리오 시뮬레이션에서 경쟁사 가격 대비 분석을 해보세요.",
                    project.getTitle(), project.getPrice(), cm,
                    hasCm ? cm.multiply(java.math.BigDecimal.valueOf(100))
                            .divide(project.getPrice(), 1, java.math.RoundingMode.HALF_UP) : "0");
        }

        // 목표/달성 관련
        if (q.contains("목표") || q.contains("달성") || q.contains("얼마나") || q.contains("필요")) {
            if (!hasCm) {
                return "현재 공헌이익이 0 이하이므로 목표 달성이 불가능합니다. 가격 인상을 먼저 검토하세요.";
            }
            java.math.BigDecimal laborCost = project.getHourlyWage()
                    .multiply(java.math.BigDecimal.valueOf(project.getWorkHours()));
            java.math.BigDecimal totalNeeded = project.getFixedCost().add(laborCost);
            java.math.BigDecimal neededQty = totalNeeded
                    .divide(cm, 1, java.math.RoundingMode.HALF_UP);
            return String.format("프로젝트 '%s'의 목표 분석입니다.\n\n" +
                    "- 월 고정비: %s원\n" +
                    "- 월 인건비(기회비용): %s원\n" +
                    "- 합계 필요 금액: %s원\n" +
                    "- 공헌이익: %s원/개\n\n" +
                    "→ 인건비 포함 손익분기: 약 %s개/월\n" +
                    "이 이상 판매해야 시급 %s원 이상의 실질 수익이 발생합니다.",
                    project.getTitle(), project.getFixedCost(), laborCost,
                    totalNeeded, cm, neededQty, project.getHourlyWage());
        }

        // 요약/종합
        if (q.contains("요약") || q.contains("종합") || q.contains("전체") || q.contains("현황") || q.contains("상태")) {
            java.math.BigDecimal bepVal = hasCm
                    ? project.getFixedCost().divide(cm, 1, java.math.RoundingMode.HALF_UP)
                    : java.math.BigDecimal.ZERO;
            java.math.BigDecimal cmRate = hasCm
                    ? cm.multiply(java.math.BigDecimal.valueOf(100))
                        .divide(project.getPrice(), 1, java.math.RoundingMode.HALF_UP)
                    : java.math.BigDecimal.ZERO;
            return String.format("프로젝트 '%s' 종합 요약입니다.\n\n" +
                    "📊 핵심 지표\n" +
                    "- 판매가: %s원 | 변동비: %s원\n" +
                    "- 공헌이익: %s원 (공헌이익률 %s%%)\n" +
                    "- 고정비: %s원/월\n" +
                    "- 손익분기점: %s개\n" +
                    "- 시급: %s원 | 월 %d시간\n\n" +
                    "%s",
                    project.getTitle(),
                    project.getPrice(), project.getVariableCost(),
                    cm, cmRate, project.getFixedCost(), bepVal,
                    project.getHourlyWage(), project.getWorkHours(),
                    hasCm ? "더 자세한 분석은 대시보드에서 확인하세요." : "⚠️ 공헌이익이 0 이하입니다. 가격 조정이 필요합니다.");
        }

        return String.format("'%s' 프로젝트에 대해 궁금하신 점이 있으시군요.\n\n" +
                "이런 질문을 해보세요:\n" +
                "- \"손익분기점이 몇 개야?\"\n" +
                "- \"실질 시급은 얼마야?\"\n" +
                "- \"비용을 줄일 방법이 있을까?\"\n" +
                "- \"가격을 올려도 될까?\"\n" +
                "- \"수익 구조는 어때?\"\n" +
                "- \"전체 현황 요약해줘\"\n" +
                "- \"목표 달성하려면 얼마나 팔아야 해?\"\n" +
                "- \"개선 전략을 추천해줘\"",
                project.getTitle());
    }
}
