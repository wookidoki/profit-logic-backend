package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.CostDetail;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.dto.CostDetailCreateRequest;
import com.wookidoki.profitlogic.dto.CostDetailResponse;
import com.wookidoki.profitlogic.repository.CostDetailRepository;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CostDetailService {

    private final CostDetailRepository costDetailRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public CostDetailResponse create(Long userId, Long projectId, CostDetailCreateRequest request) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);

        CostDetail costDetail = CostDetail.builder()
                .project(project)
                .category(request.getCategory())
                .costName(request.getCostName())
                .costType(request.getCostType())
                .amount(request.getAmount())
                .memo(request.getMemo())
                .build();

        return CostDetailResponse.from(costDetailRepository.save(costDetail));
    }

    @Transactional(readOnly = true)
    public List<CostDetailResponse> getByProject(Long userId, Long projectId) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);

        return costDetailRepository.findByProjectId(projectId).stream()
                .map(CostDetailResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long costDetailId, Long userId) {
        CostDetail costDetail = costDetailRepository.findById(costDetailId)
                .orElseThrow(() -> new ResourceNotFoundException("비용 항목", costDetailId));

        if (!costDetail.getProject().getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }

        costDetailRepository.delete(costDetail);
    }

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", projectId));
    }

    private void validateOwnership(Project project, Long userId) {
        if (!project.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }
    }
}
