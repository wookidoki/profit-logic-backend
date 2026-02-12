package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.report.ReportResponse;
import com.wookidoki.profitlogic.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/projects/{projectId}/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ResponseData<ReportResponse>> generateReport(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId,
            @RequestParam String yearMonth) {
        ReportResponse result = reportService.generateReport(projectId, yearMonth, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseData.success(result));
    }

    @GetMapping
    public ResponseEntity<ResponseData<List<ReportResponse>>> getReports(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId) {
        List<ReportResponse> result = reportService.getReports(projectId, userId);
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ResponseData<ReportResponse>> getReport(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId,
            @PathVariable Long reportId) {
        ReportResponse result = reportService.getReport(projectId, reportId, userId);
        return ResponseEntity.ok(ResponseData.success(result));
    }
}
