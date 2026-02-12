package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.CostCategory;
import com.wookidoki.profitlogic.domain.CostDetail;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.cost.CostDetailCreateRequest;
import com.wookidoki.profitlogic.dto.cost.CostDetailResponse;
import com.wookidoki.profitlogic.repository.CostDetailRepository;
import com.wookidoki.profitlogic.repository.ProjectRepository;
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
@DisplayName("CostDetailService 단위 테스트")
class CostDetailServiceTest {

    @Mock private CostDetailRepository costDetailRepository;
    @Mock private ProjectRepository projectRepository;

    @InjectMocks
    private CostDetailService costDetailService;

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
    @DisplayName("비용 항목 등록")
    class Create {

        @Test
        @DisplayName("소유자가 비용 항목 등록 → 성공")
        void create_owner_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(costDetailRepository.save(any(CostDetail.class))).willAnswer(inv -> {
                CostDetail c = inv.getArgument(0);
                return CostDetail.builder()
                        .id(1L).project(c.getProject())
                        .category(c.getCategory()).costName(c.getCostName())
                        .costType(c.getCostType()).amount(c.getAmount())
                        .memo(c.getMemo()).build();
            });

            CostDetailCreateRequest request = CostDetailCreateRequest.builder()
                    .category(CostCategory.API_USAGE)
                    .costName("OpenAI API")
                    .costType("VARIABLE")
                    .amount(new BigDecimal("15000"))
                    .memo("월간 API 사용료")
                    .build();

            CostDetailResponse result = costDetailService.create(1L, 10L, request);

            assertThat(result.getCostName()).isEqualTo("OpenAI API");
            assertThat(result.getCategory()).isEqualTo(CostCategory.API_USAGE);
            assertThat(result.getCostType()).isEqualTo("VARIABLE");
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("15000"));
            verify(costDetailRepository).save(any(CostDetail.class));
        }

        @Test
        @DisplayName("비소유자가 비용 항목 등록 시도 → UnauthorizedAccessException")
        void create_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            CostDetailCreateRequest request = CostDetailCreateRequest.builder()
                    .category(CostCategory.SERVER)
                    .costName("AWS EC2")
                    .costType("FIXED")
                    .amount(new BigDecimal("50000"))
                    .build();

            assertThatThrownBy(() -> costDetailService.create(999L, 10L, request))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verify(costDetailRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 프로젝트 → ResourceNotFoundException")
        void create_projectNotFound_throws() {
            given(projectRepository.findById(999L)).willReturn(Optional.empty());

            CostDetailCreateRequest request = CostDetailCreateRequest.builder()
                    .category(CostCategory.MATERIAL)
                    .costName("재료비")
                    .costType("VARIABLE")
                    .amount(new BigDecimal("5000"))
                    .build();

            assertThatThrownBy(() -> costDetailService.create(1L, 999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("프로젝트별 비용 항목 조회")
    class GetByProject {

        @Test
        @DisplayName("소유자가 조회 → 목록 반환")
        void getByProject_owner_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            CostDetail cost = CostDetail.builder()
                    .id(1L).project(project)
                    .category(CostCategory.TOOL_SUBSCRIPTION)
                    .costName("Figma").costType("FIXED")
                    .amount(new BigDecimal("13000")).build();

            given(projectRepository.findById(10L)).willReturn(Optional.of(project));
            given(costDetailRepository.findByProjectId(10L)).willReturn(List.of(cost));

            List<CostDetailResponse> result = costDetailService.getByProject(1L, 10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCostName()).isEqualTo("Figma");
        }

        @Test
        @DisplayName("비소유자가 조회 → UnauthorizedAccessException")
        void getByProject_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            given(projectRepository.findById(10L)).willReturn(Optional.of(project));

            assertThatThrownBy(() -> costDetailService.getByProject(999L, 10L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    @Nested
    @DisplayName("비용 항목 삭제")
    class Delete {

        @Test
        @DisplayName("소유자가 삭제 → 성공")
        void delete_owner_success() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            CostDetail cost = CostDetail.builder()
                    .id(100L).project(project)
                    .category(CostCategory.MARKETING)
                    .costName("광고비").costType("VARIABLE")
                    .amount(new BigDecimal("30000")).build();
            given(costDetailRepository.findById(100L)).willReturn(Optional.of(cost));

            costDetailService.delete(100L, 1L);

            verify(costDetailRepository).delete(cost);
        }

        @Test
        @DisplayName("비소유자가 삭제 시도 → UnauthorizedAccessException")
        void delete_notOwner_throws() {
            User user = createUser(1L);
            Project project = createProject(10L, user);
            CostDetail cost = CostDetail.builder()
                    .id(100L).project(project)
                    .category(CostCategory.OUTSOURCING)
                    .costName("외주비").costType("VARIABLE")
                    .amount(new BigDecimal("200000")).build();
            given(costDetailRepository.findById(100L)).willReturn(Optional.of(cost));

            assertThatThrownBy(() -> costDetailService.delete(100L, 999L))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verify(costDetailRepository, never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않는 비용 항목 삭제 → ResourceNotFoundException")
        void delete_notFound_throws() {
            given(costDetailRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> costDetailService.delete(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
