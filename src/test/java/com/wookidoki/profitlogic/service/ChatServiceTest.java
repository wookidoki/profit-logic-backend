package com.wookidoki.profitlogic.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 단위 테스트")
class ChatServiceTest {

    @Mock private ChatLogRepository chatLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;

    @InjectMocks
    private ChatService chatService;

    private User createUser(Long id) {
        return User.builder().id(id).email("user@test.com").password("encoded").nickname("테스터").build();
    }

    private Project createProject(Long id, User user) {
        return Project.builder()
                .id(id).user(user).title("프로젝트")
                .price(new BigDecimal("3000")).variableCost(new BigDecimal("100"))
                .fixedCost(new BigDecimal("50000")).workHours(160)
                .hourlyWage(new BigDecimal("9860"))
                .build();
    }

    @Nested
    @DisplayName("채팅 메시지 생성")
    class Chat {

        @Test
        @DisplayName("BEP 관련 질문 → 규칙 기반 응답 생성")
        void chat_bepQuestion_returnsRuleBasedAnswer() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(chatLogRepository.save(any(ChatLog.class))).willAnswer(inv -> {
                ChatLog log = inv.getArgument(0);
                return ChatLog.builder().id(1L).user(log.getUser()).project(log.getProject())
                        .question(log.getQuestion()).answer(log.getAnswer()).build();
            });

            ChatRequest request = ChatRequest.builder().projectId(10L).question("손익분기점이 궁금합니다").build();
            ChatResponse result = chatService.chat(1L, request);

            assertThat(result.getAnswer()).contains("손익분기점");
            verify(chatLogRepository).save(any(ChatLog.class));
        }

        @Test
        @DisplayName("시급 관련 질문 → 시급 정보 응답")
        void chat_wageQuestion_returnsWageInfo() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(chatLogRepository.save(any(ChatLog.class))).willAnswer(inv -> {
                ChatLog log = inv.getArgument(0);
                return ChatLog.builder().id(1L).user(log.getUser()).project(log.getProject())
                        .question(log.getQuestion()).answer(log.getAnswer()).build();
            });

            ChatRequest request = ChatRequest.builder().projectId(10L).question("내 시급은 얼마인가요?").build();
            ChatResponse result = chatService.chat(1L, request);

            assertThat(result.getAnswer()).contains("시급");
        }

        @Test
        @DisplayName("비소유 프로젝트에 채팅 시도 → UnauthorizedAccessException")
        void chat_notOwner_throws() {
            User user = createUser(1L);
            User other = createUser(2L);
            Project project = createProject(10L, user);
            given(userRepository.findById(2L)).willReturn(Optional.of(other));
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            ChatRequest request = ChatRequest.builder().projectId(10L).question("질문").build();

            assertThatThrownBy(() -> chatService.chat(2L, request))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 프로젝트 → ResourceNotFoundException")
        void chat_projectNotFound_throws() {
            User user = createUser(1L);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(projectRepository.findById(999L)).willReturn(Optional.empty());

            ChatRequest request = ChatRequest.builder().projectId(999L).question("질문").build();

            assertThatThrownBy(() -> chatService.chat(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("채팅 히스토리 조회")
    class GetHistory {

        @Test
        @DisplayName("소유자가 히스토리 조회 → 성공")
        void getHistory_owner_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            ChatLog log = ChatLog.builder().id(1L).user(user).project(project)
                    .question("질문").answer("답변").build();

            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(chatLogRepository.findByUserIdAndProjectIdOrderByCreatedAtDesc(1L, 10L))
                    .willReturn(List.of(log));

            List<ChatResponse> result = chatService.getHistory(1L, 10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getQuestion()).isEqualTo("질문");
        }

        @Test
        @DisplayName("비소유자가 히스토리 조회 → UnauthorizedAccessException")
        void getHistory_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            assertThatThrownBy(() -> chatService.getHistory(999L, 10L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }
}
