package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.cost.CostDetailCreateRequest;
import com.wookidoki.profitlogic.dto.cost.CostDetailResponse;
import com.wookidoki.profitlogic.service.CostDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CostDetailController {

    private final CostDetailService costDetailService;

    @PostMapping("/v1/projects/{projectId}/costs")
    public ResponseEntity<ResponseData<CostDetailResponse>> create(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody CostDetailCreateRequest request) {
        CostDetailResponse result = costDetailService.create(userId, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseData.success(result, "비용 항목이 등록되었습니다."));
    }

    @GetMapping("/v1/projects/{projectId}/costs")
    public ResponseEntity<ResponseData<List<CostDetailResponse>>> getByProject(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId) {
        List<CostDetailResponse> result = costDetailService.getByProject(userId, projectId);
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @DeleteMapping("/v1/costs/{costDetailId}")
    public ResponseEntity<ResponseData<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long costDetailId) {
        costDetailService.delete(costDetailId, userId);
        return ResponseEntity.ok(ResponseData.success(null, "비용 항목이 삭제되었습니다."));
    }
}
