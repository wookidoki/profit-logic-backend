package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.Simulation;
import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.SimulationCreateRequest;
import com.wookidoki.profitlogic.dto.SimulationResponse;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.SimulationRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimulationService 단위 테스트")
class SimulationServiceTest {

    @Mock private SimulationRepository simulationRepository;
    @Mock private ProjectRepository projectRepository;

    @InjectMocks
    private SimulationService simulationService;

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
    @DisplayName("시뮬레이션 저장")
    class Save {

        @Test
        @DisplayName("소유자가 시뮬레이션 저장 → 성공")
        void save_owner_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(simulationRepository.save(any(Simulation.class))).willAnswer(inv -> {
                Simulation s = inv.getArgument(0);
                return Simulation.builder().id(1L).project(s.getProject())
                        .scenarioName(s.getScenarioName()).resultJson(s.getResultJson()).build();
            });

            SimulationCreateRequest request = SimulationCreateRequest.builder()
                    .projectId(10L).scenarioName("기본 시나리오").resultJson("{\"bep\":18}").build();

            SimulationResponse result = simulationService.save(1L, request);

            assertThat(result.getScenarioName()).isEqualTo("기본 시나리오");
            verify(simulationRepository).save(any(Simulation.class));
        }

        @Test
        @DisplayName("비소유자가 시뮬레이션 저장 시도 → UnauthorizedAccessException")
        void save_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            SimulationCreateRequest request = SimulationCreateRequest.builder()
                    .projectId(10L).scenarioName("시나리오").resultJson("{}").build();

            assertThatThrownBy(() -> simulationService.save(999L, request))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verify(simulationRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 프로젝트 → ResourceNotFoundException")
        void save_projectNotFound_throws() {
            given(projectRepository.findById(999L)).willReturn(Optional.empty());

            SimulationCreateRequest request = SimulationCreateRequest.builder()
                    .projectId(999L).scenarioName("시나리오").resultJson("{}").build();

            assertThatThrownBy(() -> simulationService.save(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("프로젝트별 시뮬레이션 조회")
    class GetByProject {

        @Test
        @DisplayName("소유자가 조회 → 목록 반환")
        void getByProject_owner_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            Simulation sim = Simulation.builder().id(1L).project(project)
                    .scenarioName("시나리오1").resultJson("{}").build();

            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(simulationRepository.findByProjectId(10L)).willReturn(List.of(sim));

            List<SimulationResponse> result = simulationService.getByProject(1L, 10L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("비소유자가 조회 → UnauthorizedAccessException")
        void getByProject_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            assertThatThrownBy(() -> simulationService.getByProject(999L, 10L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    @Nested
    @DisplayName("시뮬레이션 삭제")
    class Delete {

        @Test
        @DisplayName("소유자가 삭제 → 성공")
        void delete_owner_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            Simulation sim = Simulation.builder().id(100L).project(project)
                    .scenarioName("시나리오").resultJson("{}").build();
            given(simulationRepository.findById(100L)).willReturn(Optional.of(sim));

            simulationService.delete(100L, 1L);

            verify(simulationRepository).delete(sim);
        }

        @Test
        @DisplayName("비소유자가 삭제 시도 → UnauthorizedAccessException")
        void delete_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            Simulation sim = Simulation.builder().id(100L).project(project)
                    .scenarioName("시나리오").resultJson("{}").build();
            given(simulationRepository.findById(100L)).willReturn(Optional.of(sim));

            assertThatThrownBy(() -> simulationService.delete(100L, 999L))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verify(simulationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않는 시뮬레이션 삭제 → ResourceNotFoundException")
        void delete_notFound_throws() {
            given(simulationRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> simulationService.delete(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
