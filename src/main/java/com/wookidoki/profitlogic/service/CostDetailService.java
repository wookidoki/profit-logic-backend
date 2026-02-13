package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.CostDetail;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.dto.cost.CostDetailCreateRequest;
import com.wookidoki.profitlogic.dto.cost.CostDetailResponse;
import com.wookidoki.profitlogic.dto.cost.CsvUploadResponse;
import com.wookidoki.profitlogic.repository.CostDetailRepository;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostDetailService {

    private final CostDetailRepository costDetailRepository;
    private final ProjectRepository projectRepository;
    private final CsvCostParseService csvCostParseService;

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

    @Transactional
    public CsvUploadResponse uploadCsv(Long userId, Long projectId, String csvContent) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);

        CsvCostParseService.ParseResult parseResult = csvCostParseService.parse(csvContent);
        List<CostDetailCreateRequest> parsedItems = parseResult.items();
        List<String> errors = new ArrayList<>(parseResult.errors());

        List<CostDetailResponse> importedItems = new ArrayList<>();
        int skippedCount = 0;

        for (CostDetailCreateRequest item : parsedItems) {
            try {
                CostDetail costDetail = CostDetail.builder()
                        .project(project)
                        .category(item.getCategory())
                        .costName(item.getCostName())
                        .costType(item.getCostType())
                        .amount(item.getAmount())
                        .memo(item.getMemo())
                        .build();
                importedItems.add(CostDetailResponse.from(costDetailRepository.save(costDetail)));
            } catch (Exception e) {
                skippedCount++;
                errors.add("'" + item.getCostName() + "' 저장 실패: " + e.getMessage());
            }
        }

        log.info("CSV 업로드 완료 - 프로젝트: {}, 파싱: {}건, 저장: {}건, 스킵: {}건",
                projectId, parsedItems.size(), importedItems.size(), skippedCount);

        return CsvUploadResponse.builder()
                .totalRows(parsedItems.size() + skippedCount)
                .importedCount(importedItems.size())
                .skippedCount(skippedCount)
                .importedItems(importedItems)
                .errors(errors)
                .build();
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
