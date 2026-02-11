package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.ProjectCreateRequest;
import com.wookidoki.profitlogic.dto.ProjectResponse;
import com.wookidoki.profitlogic.repository.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService 단위 테스트")
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimulationRepository simulationRepository;
    @Mock private SimulationLogRepository simulationLogRepository;
    @Mock private ChatLogRepository chatLogRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private BoardPostRepository boardPostRepository;

    @InjectMocks
    private ProjectService projectService;

    private User createUser(Long id) {
        return User.builder().id(id).email("test@test.com").password("encoded").nickname("테스터").build();
    }

    private Project createProject(Long id, User user) {
        return Project.builder()
                .id(id).user(user).title("테스트 프로젝트")
                .price(new BigDecimal("3000")).variableCost(new BigDecimal("100"))
                .fixedCost(new BigDecimal("50000")).workHours(160)
                .hourlyWage(new BigDecimal("9860")).isPublic(false)
                .build();
    }

    @Nested
    @DisplayName("프로젝트 생성")
    class Create {

        @Test
        @DisplayName("정상 생성 → ProjectResponse 반환")
        void create_success() {
            User user = createUser(1L);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(projectRepository.save(any(Project.class))).willAnswer(inv -> inv.getArgument(0));

            ProjectCreateRequest request = ProjectCreateRequest.builder()
                    .title("새 프로젝트").price(new BigDecimal("5000"))
                    .variableCost(new BigDecimal("2000")).fixedCost(new BigDecimal("100000"))
                    .workHours(200).hourlyWage(new BigDecimal("12000"))
                    .build();

            ProjectResponse result = projectService.create(1L, request);

            assertThat(result.getTitle()).isEqualTo("새 프로젝트");
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 → ResourceNotFoundException")
        void create_userNotFound_throws() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            ProjectCreateRequest request = ProjectCreateRequest.builder()
                    .title("프로젝트").price(BigDecimal.ONE).variableCost(BigDecimal.ZERO)
                    .fixedCost(BigDecimal.ZERO).workHours(1).hourlyWage(BigDecimal.ONE)
                    .build();

            assertThatThrownBy(() -> projectService.create(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("내 프로젝트 목록")
    class GetMyProjects {

        @Test
        @DisplayName("사용자의 프로젝트 리스트 반환")
        void getMyProjects_success() {
            User user = createUser(1L);
            List<Project> projects = List.of(createProject(1L, user), createProject(2L, user));
            given(projectRepository.findByUserId(1L)).willReturn(projects);

            List<ProjectResponse> result = projectService.getMyProjects(1L);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("프로젝트 단건 조회")
    class GetById {

        @Test
        @DisplayName("소유자가 조회 → 성공")
        void getById_owner_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            ProjectResponse result = projectService.getById(10L, 1L);

            assertThat(result.getTitle()).isEqualTo("테스트 프로젝트");
        }

        @Test
        @DisplayName("비소유자가 조회 → UnauthorizedAccessException")
        void getById_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.getById(10L, 999L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 프로젝트 → ResourceNotFoundException")
        void getById_notFound_throws() {
            given(projectRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.getById(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("프로젝트 삭제")
    class Delete {

        @Test
        @DisplayName("소유자가 삭제 → 자식 테이블 모두 삭제")
        void delete_success_cascadesAll() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            projectService.delete(10L, 1L);

            verify(commentRepository).deleteByProjectId(10L);
            verify(boardPostRepository).deleteByProjectId(10L);
            verify(simulationLogRepository).deleteByProjectId(10L);
            verify(simulationRepository).deleteByProjectId(10L);
            verify(chatLogRepository).deleteByProjectId(10L);
            verify(projectRepository).delete(project);
        }

        @Test
        @DisplayName("비소유자가 삭제 시도 → UnauthorizedAccessException")
        void delete_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.delete(10L, 999L))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verify(projectRepository, never()).delete(any());
        }
    }
}
