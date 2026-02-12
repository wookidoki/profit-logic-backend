package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.client.LlmClient;
import com.wookidoki.profitlogic.client.LlmResponse;
import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.ChatLog;
import com.wookidoki.profitlogic.domain.CreatorCategory;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.chat.ChatRequest;
import com.wookidoki.profitlogic.dto.chat.ChatResponse;
import com.wookidoki.profitlogic.repository.ChatLogRepository;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 단위 테스트")
class ChatServiceTest {

    @Mock private ChatLogRepository chatLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private LlmClient llmClient;

    @InjectMocks
    private ChatService chatService;

    private User createUser(Long id) {
        return User.builder().id(id).email("user@test.com")
                .password("encoded").nickname("테스터").build();
    }

    private Project createProject(Long id, User user) {
        return Project.builder()
                .id(id).user(user).title("이모티콘 프로젝트")
                .price(new BigDecimal("10000"))
                .variableCost(new BigDecimal("2000"))
                .fixedCost(new BigDecimal("500000"))
                .workHours(160)
                .hourlyWage(new BigDecimal("9860"))
                .creatorCategory(CreatorCategory.EMOTICON)
                .build();
    }

    @Nested
    @DisplayName("LLM 연동 채팅")
    class LlmChat {

        @Test
        @DisplayName("LLM 사용 가능 → LLM 응답 반환 + 토큰 기록")
        void shouldUseLlmWhenAvailable() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            ChatRequest request = ChatRequest.builder()
                    .projectId(10L).question("월 최소 몇 건을 해야 해?").build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(llmClient.isAvailable()).willReturn(true);
            given(llmClient.chatWithUsage(anyString(), anyString()))
                    .willReturn(LlmResponse.builder()
                            .content("월 최소 63건을 달성해야 합니다.")
                            .inputTokens(150).outputTokens(30).totalTokens(180).build());
            given(chatLogRepository.save(any(ChatLog.class)))
                    .willAnswer(invocation -> {
                        ChatLog saved = invocation.getArgument(0);
                        return ChatLog.builder()
                                .id(1L).user(saved.getUser()).project(saved.getProject())
                                .question(saved.getQuestion()).answer(saved.getAnswer())
                                .tokensUsed(saved.getTokensUsed()).build();
                    });

            ChatResponse response = chatService.chat(1L, request);

            assertThat(response.getAnswer()).isEqualTo("월 최소 63건을 달성해야 합니다.");
            assertThat(response.getTokensUsed()).isEqualTo(180);

            ArgumentCaptor<ChatLog> captor = ArgumentCaptor.forClass(ChatLog.class);
            verify(chatLogRepository).save(captor.capture());
            assertThat(captor.getValue().getTokensUsed()).isEqualTo(180);
        }

        @Test
        @DisplayName("LLM 미설정 → 규칙 기반 응답 + 토큰 0")
        void shouldFallbackToRuleBasedWhenLlmUnavailable() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            ChatRequest request = ChatRequest.builder()
                    .projectId(10L).question("손익분기점이 궁금해요").build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(llmClient.isAvailable()).willReturn(false);
            given(chatLogRepository.save(any(ChatLog.class)))
                    .willAnswer(invocation -> {
                        ChatLog saved = invocation.getArgument(0);
                        return ChatLog.builder()
                                .id(1L).user(saved.getUser()).project(saved.getProject())
                                .question(saved.getQuestion()).answer(saved.getAnswer())
                                .tokensUsed(saved.getTokensUsed()).build();
                    });

            ChatResponse response = chatService.chat(1L, request);

            assertThat(response.getAnswer()).contains("고정 지출");
            assertThat(response.getTokensUsed()).isZero();
            verify(llmClient, never()).chatWithUsage(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("시스템 프롬프트 생성")
    class SystemPrompt {

        @Test
        @DisplayName("프로젝트 정보가 크리에이터 용어로 시스템 프롬프트에 포함됨")
        void shouldContainProjectInfoWithCreatorTerms() {
            User user = createUser(1L);
            Project project = createProject(10L, user);

            String prompt = chatService.buildSystemPrompt(project);

            // Section headers
            assertThat(prompt).contains("=== 현재 프로젝트 정보 ===");

            // Creator terminology
            assertThat(prompt).contains("건당 수익");
            assertThat(prompt).contains("건당 비용");
            assertThat(prompt).contains("건당 순수익");
            assertThat(prompt).contains("월 고정 지출");
            assertThat(prompt).contains("월 투입 시간");
            assertThat(prompt).contains("본업 시급");

            // Project data
            assertThat(prompt).contains("이모티콘 프로젝트");
            assertThat(prompt).contains("10,000");
            assertThat(prompt).contains("2,000");
            assertThat(prompt).contains("500,000");
            assertThat(prompt).contains("8,000");  // 건당 순수익
            assertThat(prompt).contains("160");
            assertThat(prompt).contains("9,860");
        }

        @Test
        @DisplayName("시스템 프롬프트에 FINANCIAL_ADVISOR 템플릿이 포함됨")
        void shouldContainFinancialAdvisorTemplate() {
            User user = createUser(1L);
            Project project = createProject(10L, user);

            String prompt = chatService.buildSystemPrompt(project);

            assertThat(prompt).contains("Profit Logic");
            assertThat(prompt).contains("사이드 프로젝트");
        }
    }

    @Nested
    @DisplayName("권한 및 예외")
    class Authorization {

        @Test
        @DisplayName("존재하지 않는 사용자 → ResourceNotFoundException")
        void shouldThrowForUnknownUser() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.chat(999L,
                    ChatRequest.builder().projectId(1L).question("질문").build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 프로젝트 → ResourceNotFoundException")
        void shouldThrowForUnknownProject() {
            User user = createUser(1L);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(projectRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.chat(1L,
                    ChatRequest.builder().projectId(999L).question("질문").build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("비소유자 접근 → UnauthorizedAccessException")
        void shouldThrowForNonOwner() {
            User owner = createUser(1L);
            Project project = createProject(10L, owner);
            given(userRepository.findById(2L)).willReturn(Optional.of(createUser(2L)));
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            assertThatThrownBy(() -> chatService.chat(2L,
                    ChatRequest.builder().projectId(10L).question("질문").build()))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    @Nested
    @DisplayName("대화 내역 조회")
    class GetHistory {

        @Test
        @DisplayName("정상: 대화 내역 반환")
        void shouldReturnHistory() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            ChatLog log1 = ChatLog.builder().id(1L).user(user).project(project)
                    .question("질문1").answer("답변1").tokensUsed(100).build();
            ChatLog log2 = ChatLog.builder().id(2L).user(user).project(project)
                    .question("질문2").answer("답변2").tokensUsed(150).build();
            given(chatLogRepository.findByUserIdAndProjectIdOrderByCreatedAtDesc(1L, 10L))
                    .willReturn(List.of(log1, log2));

            List<ChatResponse> history = chatService.getHistory(1L, 10L);

            assertThat(history).hasSize(2);
            assertThat(history.get(0).getTokensUsed()).isEqualTo(100);
        }

        @Test
        @DisplayName("비소유자 내역 조회 → UnauthorizedAccessException")
        void shouldThrowForNonOwnerHistory() {
            User owner = createUser(1L);
            Project project = createProject(10L, owner);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            assertThatThrownBy(() -> chatService.getHistory(999L, 10L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }
}
