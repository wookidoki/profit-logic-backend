package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.client.LlmClient;
import com.wookidoki.profitlogic.client.LlmResponse;
import com.wookidoki.profitlogic.client.PromptTemplates;
import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.ChatLog;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.ChatRequest;
import com.wookidoki.profitlogic.dto.ChatResponse;
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

        if (q.contains("손익분기") || q.contains("bep")) {
            return String.format("현재 프로젝트 '%s'의 고정비는 %s원, 판매가는 %s원, 변동비는 %s원입니다. " +
                            "손익분기점 분석을 위해 대시보드의 분석 기능을 이용해보세요.",
                    project.getTitle(), project.getFixedCost(), project.getPrice(), project.getVariableCost());
        }

        if (q.contains("시급") || q.contains("급여") || q.contains("임금")) {
            return String.format("프로젝트 '%s'의 설정된 근무시간은 %d시간, 시급은 %s원입니다. " +
                            "실질 시급(Shadow Wage)은 분석 결과에서 확인할 수 있습니다.",
                    project.getTitle(), project.getWorkHours(), project.getHourlyWage());
        }

        if (q.contains("수익") || q.contains("이익") || q.contains("매출")) {
            return String.format("프로젝트 '%s'의 판매가 %s원에서 변동비 %s원을 빼면 " +
                            "단위당 공헌이익은 %s원입니다. 자세한 수익 분석은 대시보드를 확인해주세요.",
                    project.getTitle(), project.getPrice(), project.getVariableCost(),
                    project.getPrice().subtract(project.getVariableCost()));
        }

        return String.format("'%s' 프로젝트에 대해 궁금하신 점이 있으시군요. " +
                        "손익분기점, 시급, 수익성 등 구체적인 키워드로 질문해주시면 더 정확한 답변을 드릴 수 있습니다.",
                project.getTitle());
    }
}
