package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.GoalProgressResponse;
import com.wookidoki.profitlogic.dto.GoalUpdateRequest;
import com.wookidoki.profitlogic.dto.ProjectResponse;
import com.wookidoki.profitlogic.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/projects/{projectId}/goal")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<ResponseData<GoalProgressResponse>> getProgress(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId) {
        GoalProgressResponse result = goalService.getProgress(projectId, userId);
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @PutMapping
    public ResponseEntity<ResponseData<ProjectResponse>> updateGoal(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody GoalUpdateRequest request) {
        ProjectResponse result = goalService.updateGoal(projectId, userId, request);
        return ResponseEntity.ok(ResponseData.success(result));
    }
}
