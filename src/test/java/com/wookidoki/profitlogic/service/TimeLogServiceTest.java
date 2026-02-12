package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.TimeLog;
import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.timelog.TimeLogCreateRequest;
import com.wookidoki.profitlogic.dto.timelog.TimeLogResponse;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.TimeLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimeLogService 단위 테스트")
class TimeLogServiceTest {

    @Mock private TimeLogRepository timeLogRepository;
    @Mock private ProjectRepository projectRepository;

    @InjectMocks
    private TimeLogService timeLogService;

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
    @DisplayName("시간 기록 등록")
    class Create {

        @Test
        @DisplayName("소유자가 시간 기록 등록 → 성공")
        void create_owner_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(timeLogRepository.save(any(TimeLog.class))).willAnswer(inv -> {
                TimeLog t = inv.getArgument(0);
                return TimeLog.builder()
                        .id(1L).project(t.getProject())
                        .taskName(t.getTaskName()).hoursSpent(t.getHoursSpent())
                        .logDate(t.getLogDate()).memo(t.getMemo()).build();
            });

            TimeLogCreateRequest request = TimeLogCreateRequest.builder()
                    .taskName("이모티콘 스케치")
                    .hoursSpent(new BigDecimal("3.5"))
                    .logDate(LocalDate.of(2026, 1, 15))
                    .memo("초안 작업")
                    .build();

            TimeLogResponse result = timeLogService.create(1L, 10L, request);

            assertThat(result.getTaskName()).isEqualTo("이모티콘 스케치");
            assertThat(result.getHoursSpent()).isEqualByComparingTo(new BigDecimal("3.5"));
            assertThat(result.getLogDate()).isEqualTo(LocalDate.of(2026, 1, 15));
            verify(timeLogRepository).save(any(TimeLog.class));
        }

        @Test
        @DisplayName("비소유자가 등록 시도 → UnauthorizedAccessException")
        void create_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            TimeLogCreateRequest request = TimeLogCreateRequest.builder()
                    .taskName("작업").hoursSpent(new BigDecimal("2"))
                    .logDate(LocalDate.of(2026, 1, 15)).build();

            assertThatThrownBy(() -> timeLogService.create(999L, 10L, request))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verify(timeLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 프로젝트 → ResourceNotFoundException")
        void create_projectNotFound_throws() {
            given(projectRepository.findById(999L)).willReturn(Optional.empty());

            TimeLogCreateRequest request = TimeLogCreateRequest.builder()
                    .taskName("작업").hoursSpent(new BigDecimal("1"))
                    .logDate(LocalDate.of(2026, 1, 15)).build();

            assertThatThrownBy(() -> timeLogService.create(1L, 999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("프로젝트별 시간 기록 조회")
    class GetByProject {

        @Test
        @DisplayName("소유자가 조회 → 목록 반환")
        void getByProject_owner_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            TimeLog log = TimeLog.builder()
                    .id(1L).project(project).taskName("영상 편집")
                    .hoursSpent(new BigDecimal("4")).logDate(LocalDate.of(2026, 1, 10)).build();

            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(timeLogRepository.findByProjectIdOrderByLogDateDesc(10L)).willReturn(List.of(log));

            List<TimeLogResponse> result = timeLogService.getByProject(1L, 10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTaskName()).isEqualTo("영상 편집");
        }

        @Test
        @DisplayName("비소유자가 조회 → UnauthorizedAccessException")
        void getByProject_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            assertThatThrownBy(() -> timeLogService.getByProject(999L, 10L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    @Nested
    @DisplayName("기간별 시간 기록 조회")
    class GetByDateRange {

        @Test
        @DisplayName("기간 필터 조회 → 해당 기간 기록만 반환")
        void getByDateRange_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 1, 31);
            TimeLog log = TimeLog.builder()
                    .id(1L).project(project).taskName("프롬프트 작성")
                    .hoursSpent(new BigDecimal("2")).logDate(LocalDate.of(2026, 1, 20)).build();

            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(timeLogRepository.findByProjectIdAndLogDateBetween(10L, from, to)).willReturn(List.of(log));

            List<TimeLogResponse> result = timeLogService.getByProjectAndDateRange(1L, 10L, from, to);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("시간 기록 삭제")
    class Delete {

        @Test
        @DisplayName("소유자가 삭제 → 성공")
        void delete_owner_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            TimeLog log = TimeLog.builder()
                    .id(100L).project(project).taskName("작업")
                    .hoursSpent(new BigDecimal("1")).logDate(LocalDate.of(2026, 1, 10)).build();
            given(timeLogRepository.findById(100L)).willReturn(Optional.of(log));

            timeLogService.delete(100L, 1L);

            verify(timeLogRepository).delete(log);
        }

        @Test
        @DisplayName("비소유자가 삭제 시도 → UnauthorizedAccessException")
        void delete_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            TimeLog log = TimeLog.builder()
                    .id(100L).project(project).taskName("작업")
                    .hoursSpent(new BigDecimal("1")).logDate(LocalDate.of(2026, 1, 10)).build();
            given(timeLogRepository.findById(100L)).willReturn(Optional.of(log));

            assertThatThrownBy(() -> timeLogService.delete(100L, 999L))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verify(timeLogRepository, never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않는 기록 삭제 → ResourceNotFoundException")
        void delete_notFound_throws() {
            given(timeLogRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> timeLogService.delete(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("총 작업 시간 합산")
    class GetTotalHours {

        @Test
        @DisplayName("기록 있을 때 → 합산값 반환")
        void getTotalHours_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            TimeLog log1 = TimeLog.builder()
                    .id(1L).project(project).taskName("스케치")
                    .hoursSpent(new BigDecimal("3.5")).logDate(LocalDate.of(2026, 1, 10)).build();
            TimeLog log2 = TimeLog.builder()
                    .id(2L).project(project).taskName("채색")
                    .hoursSpent(new BigDecimal("4.5")).logDate(LocalDate.of(2026, 1, 11)).build();

            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(timeLogRepository.findByProjectIdOrderByLogDateDesc(10L)).willReturn(List.of(log1, log2));

            BigDecimal total = timeLogService.getTotalHours(1L, 10L);

            assertThat(total).isEqualByComparingTo(new BigDecimal("8.0"));
        }

        @Test
        @DisplayName("기록 없을 때 → 0 반환")
        void getTotalHours_empty_returnsZero() {
            User user = createUser(1L);
            Project project = createProject(10L, user);

            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(timeLogRepository.findByProjectIdOrderByLogDateDesc(10L)).willReturn(List.of());

            BigDecimal total = timeLogService.getTotalHours(1L, 10L);

            assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
