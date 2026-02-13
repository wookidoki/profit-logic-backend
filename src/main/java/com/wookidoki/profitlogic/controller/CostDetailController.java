package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.common.exception.BusinessLogicException;
import com.wookidoki.profitlogic.dto.cost.CostDetailCreateRequest;
import com.wookidoki.profitlogic.dto.cost.CostDetailResponse;
import com.wookidoki.profitlogic.dto.cost.CsvUploadResponse;
import com.wookidoki.profitlogic.service.CostDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    @PostMapping(value = "/v1/projects/{projectId}/costs/csv",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseData<CsvUploadResponse>> uploadCsv(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new BusinessLogicException("CSV 파일을 선택해주세요.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new BusinessLogicException("CSV 파일만 업로드 가능합니다.");
        }

        if (file.getSize() > 1024 * 1024) {
            throw new BusinessLogicException("파일 크기는 1MB 이하여야 합니다.");
        }

        try {
            String csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            CsvUploadResponse result = costDetailService.uploadCsv(userId, projectId, csvContent);

            String message = result.getImportedCount() + "건의 비용이 등록되었습니다.";
            if (result.getSkippedCount() > 0) {
                message += " (" + result.getSkippedCount() + "건 스킵)";
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseData.success(result, message));
        } catch (IOException e) {
            throw new BusinessLogicException("CSV 파일 읽기에 실패했습니다.");
        }
    }

    @DeleteMapping("/v1/costs/{costDetailId}")
    public ResponseEntity<ResponseData<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long costDetailId) {
        costDetailService.delete(costDetailId, userId);
        return ResponseEntity.ok(ResponseData.success(null, "비용 항목이 삭제되었습니다."));
    }
}
