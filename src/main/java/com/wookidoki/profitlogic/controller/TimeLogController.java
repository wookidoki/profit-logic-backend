package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.TimeLogCreateRequest;
import com.wookidoki.profitlogic.dto.TimeLogResponse;
import com.wookidoki.profitlogic.service.TimeLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TimeLogController {

    private final TimeLogService timeLogService;

    @PostMapping("/v1/projects/{projectId}/timelogs")
    public ResponseEntity<ResponseData<TimeLogResponse>> create(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody TimeLogCreateRequest request) {
        TimeLogResponse result = timeLogService.create(userId, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseData.success(result, "시간 기록이 등록되었습니다."));
    }

    @GetMapping("/v1/projects/{projectId}/timelogs")
    public ResponseEntity<ResponseData<List<TimeLogResponse>>> getByProject(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<TimeLogResponse> result;
        if (from != null && to != null) {
            result = timeLogService.getByProjectAndDateRange(userId, projectId, from, to);
        } else {
            result = timeLogService.getByProject(userId, projectId);
        }
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @GetMapping("/v1/projects/{projectId}/timelogs/total")
    public ResponseEntity<ResponseData<BigDecimal>> getTotalHours(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId) {
        BigDecimal totalHours = timeLogService.getTotalHours(userId, projectId);
        return ResponseEntity.ok(ResponseData.success(totalHours));
    }

    @DeleteMapping("/v1/timelogs/{timeLogId}")
    public ResponseEntity<ResponseData<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long timeLogId) {
        timeLogService.delete(timeLogId, userId);
        return ResponseEntity.ok(ResponseData.success(null, "시간 기록이 삭제되었습니다."));
    }
}
