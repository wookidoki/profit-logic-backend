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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final RoundingMode ROUND = RoundingMode.HALF_UP;

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
            try {
                String systemPrompt = buildSystemPrompt(project);
                LlmResponse llmResponse = llmClient.chatWithUsage(systemPrompt, request.getQuestion());
                answer = llmResponse.getContent();
                tokensUsed = llmResponse.getTotalTokens();
                log.info("LLM 응답 완료 - 프로젝트: {}, 토큰: {}", project.getTitle(), tokensUsed);
            } catch (Exception e) {
                log.warn("LLM 호출 실패, 규칙 기반 폴백 - 원인: {}", e.getMessage());
                answer = generateRuleBasedAnswer(request.getQuestion(), project);
            }
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

    // ══════════════════════════════════════════
    // System Prompt (LLM용)
    // ══════════════════════════════════════════

    String buildSystemPrompt(Project project) {
        BigDecimal cm = project.getPrice().subtract(project.getVariableCost());
        return PromptTemplates.FINANCIAL_ADVISOR + "\n\n" +
                "=== 현재 프로젝트 정보 ===\n" +
                "프로젝트명: " + project.getTitle() + "\n" +
                "건당 수익: " + fmt(project.getPrice()) + "원\n" +
                "건당 비용: " + fmt(project.getVariableCost()) + "원\n" +
                "건당 순수익: " + fmt(cm) + "원\n" +
                "월 고정 지출: " + fmt(project.getFixedCost()) + "원\n" +
                "월 투입 시간: " + project.getWorkHours() + "시간\n" +
                "본업 시급: " + fmt(project.getHourlyWage()) + "원";
    }

    // ══════════════════════════════════════════
    // Rule-Based Answer (LLM 미설정 시 fallback)
    // ══════════════════════════════════════════

    private String generateRuleBasedAnswer(String question, Project project) {
        String q = question.toLowerCase().trim();
        BigDecimal cm = project.getPrice().subtract(project.getVariableCost());
        boolean hasCm = cm.compareTo(BigDecimal.ZERO) > 0;

        // ── 인사/잡담/짧은 입력 ──
        if (isGreeting(q)) {
            return greetingWithSummary(project, cm, hasCm);
        }

        // ── BEP ──
        if (matchAny(q, "손익분기", "bep", "몇 개", "몇개", "몇 건", "몇건", "최소", "본전")) {
            return answerBep(project, cm, hasCm);
        }

        // ── 시급 ──
        if (matchAny(q, "시급", "급여", "임금", "shadow", "시간당")) {
            return answerWage(project, cm, hasCm);
        }

        // ── 성장/지속/가치 ──
        if (matchAny(q, "성장", "가치", "계속", "포기", "그만", "지속", "미래", "전망", "할만")) {
            return answerGrowth(project, cm, hasCm);
        }

        // ── 수익/이익 ──
        if (matchAny(q, "수익", "이익", "매출", "돈", "벌")) {
            return answerProfit(project, cm, hasCm);
        }

        // ── 비용 ──
        if (matchAny(q, "비용", "절감", "줄", "아끼", "절약", "지출")) {
            return answerCost(project);
        }

        // ── 가격 ──
        if (matchAny(q, "가격", "올려", "인상", "할인", "낮춰", "단가")) {
            return answerPrice(project, cm, hasCm);
        }

        // ── 시간 ──
        if (matchAny(q, "시간", "작업", "효율", "얼마나")) {
            return answerTime(project);
        }

        // ── 전략/조언 ──
        if (matchAny(q, "전략", "방법", "어떻게", "조언", "추천", "개선", "팁")) {
            return answerStrategy(project, cm, hasCm);
        }

        // ── 요약 ──
        if (matchAny(q, "요약", "종합", "전체", "현황", "상태", "알려")) {
            return answerSummary(project, cm, hasCm);
        }

        // ── 목표 ──
        if (matchAny(q, "목표", "달성", "필요")) {
            return answerGoal(project, cm, hasCm);
        }

        // ── 비교 ──
        if (matchAny(q, "비교", "경쟁", "시장", "평균")) {
            return answerCompare(project, cm, hasCm);
        }

        // ── 기본: 항상 데이터 포함 응답 ──
        return answerDefault(project, cm, hasCm);
    }

    // ══════════════════════════════════════════
    // 개별 응답 메서드
    // ══════════════════════════════════════════

    private String greetingWithSummary(Project project, BigDecimal cm, boolean hasCm) {
        BigDecimal bep = calcBep(project.getFixedCost(), cm, hasCm);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("안녕하세요! '%s' 프로젝트를 함께 살펴보겠습니다.\n\n", project.getTitle()));
        sb.append("📊 현재 핵심 지표:\n");
        sb.append(String.format("• 건당 수익: %s원 / 건당 비용: %s원\n", fmt(project.getPrice()), fmt(project.getVariableCost())));
        sb.append(String.format("• 건당 순수익: %s원\n", fmt(cm)));
        sb.append(String.format("• 월 고정 지출: %s원\n", fmt(project.getFixedCost())));
        if (hasCm) {
            sb.append(String.format("• 월 최소 %s건 달성해야 본전\n", bep));
        }
        sb.append(String.format("• 월 %d시간 투입 중 (본업 시급 %s원)\n\n", project.getWorkHours(), fmt(project.getHourlyWage())));

        if (!hasCm) {
            sb.append("⚠️ 건당 수익이 건당 비용보다 낮습니다. 가격 조정이 시급합니다.\n\n");
        } else if (bep.compareTo(BigDecimal.valueOf(100)) > 0) {
            sb.append("⚠️ 월 최소 건수가 높은 편입니다. 비용 절감이나 단가 인상을 고려해보세요.\n\n");
        } else {
            sb.append("✅ 수익 구조가 양호합니다!\n\n");
        }
        sb.append("궁금한 점을 편하게 물어보세요. 수익, 비용, 시급, 성장성 등 뭐든 분석해드립니다.");
        return sb.toString();
    }

    private String answerBep(Project project, BigDecimal cm, boolean hasCm) {
        if (!hasCm) {
            return String.format("'%s'의 건당 순수익이 0 이하입니다.\n건당 수익(%s원)이 건당 비용(%s원)보다 높아야 합니다.\n단가를 올리거나 건당 비용을 줄여보세요.",
                    project.getTitle(), fmt(project.getPrice()), fmt(project.getVariableCost()));
        }
        BigDecimal bep = calcBep(project.getFixedCost(), cm, true);
        return String.format("'%s'의 월 최소 건수(BEP)는 약 %s건입니다.\n\n" +
                "계산 근거:\n" +
                "• 건당 수익: %s원\n" +
                "• 건당 비용: %s원\n" +
                "• 건당 순수익: %s원\n" +
                "• 월 고정 지출: %s원\n" +
                "• BEP = %s ÷ %s = %s건\n\n" +
                "월 %s건 이상 달성하면 이익이 발생합니다.",
                project.getTitle(), bep,
                fmt(project.getPrice()), fmt(project.getVariableCost()), fmt(cm),
                fmt(project.getFixedCost()), fmt(project.getFixedCost()), fmt(cm), bep, bep);
    }

    private String answerWage(Project project, BigDecimal cm, boolean hasCm) {
        BigDecimal laborCost = project.getHourlyWage().multiply(BigDecimal.valueOf(project.getWorkHours()));
        BigDecimal realWage = BigDecimal.ZERO;
        String comparison = "";

        if (hasCm && project.getWorkHours() > 0) {
            BigDecimal bep = calcBep(project.getFixedCost(), cm, true);
            BigDecimal revenue = cm.multiply(bep).subtract(project.getFixedCost());
            realWage = revenue.divide(BigDecimal.valueOf(project.getWorkHours()), 0, ROUND);

            int ratio = project.getHourlyWage().compareTo(BigDecimal.ZERO) > 0
                    ? realWage.multiply(BigDecimal.valueOf(100)).divide(project.getHourlyWage(), 0, ROUND).intValue()
                    : 0;

            if (ratio >= 100) {
                comparison = String.format("✅ BEP 기준 실질 시급이 본업의 %d%%입니다. 시간 투자 가치가 있습니다!", ratio);
            } else if (ratio >= 50) {
                comparison = String.format("👍 BEP 기준 실질 시급이 본업의 %d%%입니다. 규모가 커지면 역전 가능합니다.", ratio);
            } else {
                comparison = String.format("⚠️ BEP 기준 실질 시급이 본업의 %d%%입니다. 구조 개선이 필요합니다.", ratio);
            }
        }

        return String.format("'%s'의 시급 분석입니다.\n\n" +
                "• 본업 시급: %s원/시간\n" +
                "• 월 투입 시간: %d시간\n" +
                "• 월 기회비용: %s원\n\n%s\n\n" +
                "프로젝트 상세 → '분석 결과' 탭에서 정확한 실질 시급을 확인할 수 있습니다.",
                project.getTitle(), fmt(project.getHourlyWage()),
                project.getWorkHours(), fmt(laborCost), comparison);
    }

    private String answerGrowth(Project project, BigDecimal cm, boolean hasCm) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("'%s'의 성장 가능성을 분석해볼게요.\n\n", project.getTitle()));

        if (!hasCm) {
            sb.append("❌ 현재 구조에서는 성장이 어렵습니다.\n");
            sb.append(String.format("건당 수익(%s원)이 건당 비용(%s원)보다 낮아서, 많이 할수록 손해가 커집니다.\n\n",
                    fmt(project.getPrice()), fmt(project.getVariableCost())));
            sb.append("먼저 단가를 올리거나 비용을 줄여야 합니다.");
            return sb.toString();
        }

        BigDecimal bep = calcBep(project.getFixedCost(), cm, true);
        BigDecimal cmRate = calcRate(cm, project.getPrice());

        sb.append("📈 성장 가능성 평가:\n\n");

        // 순수익률
        if (cmRate.compareTo(BigDecimal.valueOf(60)) > 0) {
            sb.append(String.format("✅ 건당 순수익률 %s%% — 매우 높은 마진. 규모 확장 시 수익이 빠르게 올라갑니다.\n", cmRate));
        } else if (cmRate.compareTo(BigDecimal.valueOf(30)) > 0) {
            sb.append(String.format("👍 건당 순수익률 %s%% — 양호한 마진. 꾸준히 하면 성장 가능합니다.\n", cmRate));
        } else {
            sb.append(String.format("⚠️ 건당 순수익률 %s%% — 낮은 편. 단가 인상이나 비용 절감이 필요합니다.\n", cmRate));
        }

        // BEP 난이도
        if (bep.compareTo(BigDecimal.valueOf(20)) <= 0) {
            sb.append(String.format("✅ 월 최소 %s건 — 진입 장벽이 낮아서 빠르게 이익 구간에 들어갈 수 있습니다.\n", bep));
        } else if (bep.compareTo(BigDecimal.valueOf(100)) <= 0) {
            sb.append(String.format("👍 월 최소 %s건 — 노력하면 달성 가능한 수준입니다.\n", bep));
        } else {
            sb.append(String.format("⚠️ 월 최소 %s건 — 높은 허들. 고정 지출 줄이기를 우선 고려하세요.\n", bep));
        }

        // 시급 대비
        if (project.getWorkHours() > 0 && project.getHourlyWage().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal revenue = cm.multiply(bep).subtract(project.getFixedCost());
            BigDecimal realWage = revenue.divide(BigDecimal.valueOf(project.getWorkHours()), 0, ROUND);
            int ratio = realWage.multiply(BigDecimal.valueOf(100)).divide(project.getHourlyWage(), 0, ROUND).intValue();
            sb.append(String.format("\n💰 본업 시급 대비: BEP 기준 실질 시급이 본업의 약 %d%%입니다.\n", Math.max(ratio, 0)));
            if (ratio >= 100) sb.append("→ 본업보다 시간 가치가 높습니다. 투자할 가치가 충분합니다!\n");
            else if (ratio >= 50) sb.append("→ 규모가 커지면 역전 가능합니다. 계속해볼 가치가 있습니다.\n");
            else sb.append("→ 현재 시간 대비 수익이 낮습니다. 구조 개선 후 판단하세요.\n");
        }

        sb.append("\n더 궁금한 점이 있으면 편하게 물어보세요!");
        return sb.toString();
    }

    private String answerProfit(Project project, BigDecimal cm, boolean hasCm) {
        BigDecimal cmRate = calcRate(cm, project.getPrice());
        String verdict = cmRate.compareTo(BigDecimal.valueOf(50)) > 0
                ? "건당 순수익률이 높아서 좋은 수익 구조입니다!"
                : cmRate.compareTo(BigDecimal.valueOf(20)) < 0
                    ? "건당 순수익률이 낮습니다. 단가 인상이나 건당 비용 절감을 검토해보세요."
                    : "보통 수준의 순수익률입니다. 건수를 늘리면 수익이 개선됩니다.";

        return String.format("'%s'의 수익 구조입니다.\n\n" +
                "• 건당 수익: %s원\n" +
                "• 건당 비용: %s원\n" +
                "• 건당 순수익: %s원 (순수익률 %s%%)\n" +
                "• 월 고정 지출: %s원\n\n%s",
                project.getTitle(), fmt(project.getPrice()), fmt(project.getVariableCost()),
                fmt(cm), cmRate, fmt(project.getFixedCost()), verdict);
    }

    private String answerCost(Project project) {
        return String.format("'%s'의 비용 구조를 분석해볼게요.\n\n" +
                "• 월 고정 지출: %s원 (매달 나가는 비용)\n" +
                "• 건당 비용: %s원 (건마다 드는 비용)\n\n" +
                "비용 절감 팁:\n" +
                "1. 고정 지출 절감: 구독 서비스 정리, 대안 도구 검토\n" +
                "2. 건당 비용 절감: 재료 대량 구매, 작업 효율화\n" +
                "3. '비용 상세' 탭에서 항목별 비용을 기록하면 더 정확한 분석이 가능합니다.",
                project.getTitle(), fmt(project.getFixedCost()), fmt(project.getVariableCost()));
    }

    private String answerPrice(Project project, BigDecimal cm, boolean hasCm) {
        if (!hasCm) return "현재 건당 수익이 건당 비용보다 낮습니다. 단가 인상이 필수입니다.";

        BigDecimal currentBep = calcBep(project.getFixedCost(), cm, true);
        BigDecimal newPrice = project.getPrice().multiply(BigDecimal.valueOf(1.1));
        BigDecimal newCm = newPrice.subtract(project.getVariableCost());
        BigDecimal newBep = calcBep(project.getFixedCost(), newCm, true);

        return String.format("단가 시뮬레이션 결과입니다.\n\n" +
                "현재: 건당 수익 %s원 → 월 최소 %s건\n" +
                "10%% 인상 시: 건당 수익 %s원 → 월 최소 %s건\n\n" +
                "→ 10%% 인상하면 월 최소 건수가 %s건 줄어듭니다.\n" +
                "'시나리오 시뮬레이션' 기능에서 다양한 단가를 테스트해보세요.",
                fmt(project.getPrice()), currentBep,
                fmt(newPrice.setScale(0, ROUND)), newBep,
                currentBep.subtract(newBep));
    }

    private String answerTime(Project project) {
        return String.format("'%s'의 시간 분석입니다.\n\n" +
                "• 월 %d시간 투입 중\n" +
                "• 하루 약 %.1f시간 (22일 기준)\n\n" +
                "'작업시간' 탭에서 일별 작업시간을 기록하면 실제 투입 시간 대비 수익을 더 정확하게 분석할 수 있습니다.",
                project.getTitle(), project.getWorkHours(), project.getWorkHours() / 22.0);
    }

    private String answerStrategy(Project project, BigDecimal cm, boolean hasCm) {
        BigDecimal cmRate = calcRate(cm, project.getPrice());
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("'%s' 개선 전략을 제안합니다.\n\n", project.getTitle()));

        if (!hasCm) {
            sb.append("1. [긴급] 건당 수익이 건당 비용보다 낮습니다. 단가 인상이 최우선입니다.\n");
        } else if (cmRate.compareTo(BigDecimal.valueOf(30)) < 0) {
            sb.append("1. 건당 순수익률이 낮습니다. 건당 비용 절감 또는 단가 인상을 고려하세요.\n");
        } else {
            sb.append("1. 건당 순수익률이 양호합니다. 건수 확대에 집중하세요.\n");
        }

        if (project.getFixedCost().compareTo(BigDecimal.valueOf(500000)) > 0) {
            sb.append("2. 월 고정 지출이 높은 편입니다. 불필요한 구독/렌탈 비용을 점검하세요.\n");
        } else {
            sb.append("2. 월 고정 지출은 적정 수준입니다.\n");
        }

        sb.append("3. '시나리오 시뮬레이션'에서 단가/비용 변경 효과를 미리 확인해보세요.\n");
        sb.append("4. '트렌드' 탭에서 월별 추이를 확인하세요.");
        return sb.toString();
    }

    private String answerSummary(Project project, BigDecimal cm, boolean hasCm) {
        BigDecimal bep = calcBep(project.getFixedCost(), cm, hasCm);
        BigDecimal cmRate = calcRate(cm, project.getPrice());

        return String.format("'%s' 종합 요약입니다.\n\n" +
                "📊 핵심 지표\n" +
                "• 건당 수익: %s원 | 건당 비용: %s원\n" +
                "• 건당 순수익: %s원 (순수익률 %s%%)\n" +
                "• 월 고정 지출: %s원\n" +
                "• 월 최소 건수: %s건\n" +
                "• 본업 시급: %s원 | 월 %d시간\n\n%s",
                project.getTitle(),
                fmt(project.getPrice()), fmt(project.getVariableCost()),
                fmt(cm), cmRate, fmt(project.getFixedCost()), bep,
                fmt(project.getHourlyWage()), project.getWorkHours(),
                hasCm ? "더 자세한 분석은 프로젝트 상세에서 확인하세요." : "⚠️ 건당 순수익이 0 이하입니다. 단가 조정이 필요합니다.");
    }

    private String answerGoal(Project project, BigDecimal cm, boolean hasCm) {
        if (!hasCm) return "건당 순수익이 0 이하여서 목표 달성이 불가능합니다. 단가 인상을 먼저 검토하세요.";

        BigDecimal laborCost = project.getHourlyWage().multiply(BigDecimal.valueOf(project.getWorkHours()));
        BigDecimal totalNeeded = project.getFixedCost().add(laborCost);
        BigDecimal neededQty = totalNeeded.divide(cm, 1, ROUND);

        return String.format("'%s'의 목표 분석입니다.\n\n" +
                "• 월 고정 지출: %s원\n" +
                "• 월 기회비용(본업 시급 기준): %s원\n" +
                "• 합계 필요 금액: %s원\n" +
                "• 건당 순수익: %s원\n\n" +
                "→ 기회비용 포함 손익분기: 약 %s건/월\n" +
                "이 이상 달성해야 본업 시급(%s원) 이상의 실질 수익이 발생합니다.",
                project.getTitle(), fmt(project.getFixedCost()), fmt(laborCost),
                fmt(totalNeeded), fmt(cm), neededQty, fmt(project.getHourlyWage()));
    }

    private String answerCompare(Project project, BigDecimal cm, boolean hasCm) {
        BigDecimal cmRate = calcRate(cm, project.getPrice());
        return String.format("'%s'의 현재 지표 요약입니다.\n\n" +
                "• 건당 수익: %s원\n" +
                "• 건당 순수익: %s원\n" +
                "• 건당 순수익률: %s%%\n\n" +
                "일반적으로 순수익률 30%% 이상이면 건전한 수익 구조입니다.\n" +
                "'시나리오 시뮬레이션'에서 경쟁 상황 대비 분석을 해보세요.",
                project.getTitle(), fmt(project.getPrice()), fmt(cm), cmRate);
    }

    private String answerDefault(Project project, BigDecimal cm, boolean hasCm) {
        BigDecimal bep = calcBep(project.getFixedCost(), cm, hasCm);
        BigDecimal cmRate = calcRate(cm, project.getPrice());

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("'%s' 프로젝트에 대해 분석해볼게요.\n\n", project.getTitle()));
        sb.append("📊 핵심 수치:\n");
        sb.append(String.format("• 건당 순수익: %s원 (순수익률 %s%%)\n", fmt(cm), cmRate));
        if (hasCm) sb.append(String.format("• 월 최소 %s건 달성해야 본전\n", bep));
        sb.append(String.format("• 월 %d시간 투입, 본업 시급 %s원\n\n", project.getWorkHours(), fmt(project.getHourlyWage())));

        if (!hasCm) sb.append("🚨 건당 수익이 건당 비용보다 낮습니다. 단가 인상이 시급합니다.");
        else if (bep.compareTo(BigDecimal.valueOf(100)) > 0) sb.append("💡 월 최소 건수가 높습니다. 월 고정 지출을 줄이면 수월해집니다.");
        else if (cmRate.compareTo(BigDecimal.valueOf(50)) > 0) sb.append("💡 마진이 높은 좋은 구조입니다. 건수를 늘리는 데 집중하세요.");
        else sb.append("💡 안정적인 구조입니다. 꾸준히 유지하면서 효율을 높여보세요.");

        sb.append("\n\n수익성, 비용, 시급, 성장성 등 더 궁금한 점을 물어보세요!");
        return sb.toString();
    }

    // ══════════════════════════════════════════
    // 유틸리티
    // ══════════════════════════════════════════

    private boolean isGreeting(String q) {
        if (q.length() <= 3) return true;
        return q.matches(".*(안녕|반가|하이|hello|hi|헬로|ㅎㅇ|처음|시작).*");
    }

    private boolean matchAny(String q, String... keywords) {
        for (String kw : keywords) {
            if (q.contains(kw)) return true;
        }
        return false;
    }

    private BigDecimal calcBep(BigDecimal fixedCost, BigDecimal cm, boolean hasCm) {
        if (!hasCm || cm.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return fixedCost.divide(cm, 1, ROUND);
    }

    private BigDecimal calcRate(BigDecimal part, BigDecimal whole) {
        if (whole.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, ROUND);
    }

    private String fmt(BigDecimal value) {
        if (value == null) return "0";
        return String.format("%,d", value.setScale(0, ROUND).longValue());
    }
}
