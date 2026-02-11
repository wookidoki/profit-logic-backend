package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.ProjectCreateRequest;
import com.wookidoki.profitlogic.dto.ProjectResponse;
import com.wookidoki.profitlogic.dto.ProjectUpdateRequest;
import com.wookidoki.profitlogic.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ResponseData<ProjectResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProjectCreateRequest request) {
        ProjectResponse result = projectService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseData.success(result, "프로젝트가 생성되었습니다."));
    }

    @GetMapping
    public ResponseEntity<ResponseData<List<ProjectResponse>>> getMyProjects(
            @AuthenticationPrincipal Long userId) {
        List<ProjectResponse> result = projectService.getMyProjects(userId);
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ResponseData<ProjectResponse>> getById(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId) {
        ProjectResponse result = projectService.getById(projectId, userId);
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ResponseData<ProjectResponse>> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequest request) {
        ProjectResponse result = projectService.update(projectId, userId, request);
        return ResponseEntity.ok(ResponseData.success(result, "프로젝트가 수정되었습니다."));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ResponseData<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId) {
        projectService.delete(projectId, userId);
        return ResponseEntity.ok(ResponseData.success(null, "프로젝트가 삭제되었습니다."));
    }
}
