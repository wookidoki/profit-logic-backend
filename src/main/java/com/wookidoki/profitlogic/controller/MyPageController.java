package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.mypage.*;
import com.wookidoki.profitlogic.service.MyPageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/my-page")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping("/profile")
    public ResponseEntity<ResponseData<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ResponseData.success(myPageService.getProfile(userId)));
    }

    @GetMapping("/daily-logs/{date}")
    public ResponseEntity<ResponseData<DailyLogResponse>> getDailyLog(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ResponseData.success(myPageService.getDailyLog(userId, date)));
    }

    @GetMapping("/daily-logs")
    public ResponseEntity<ResponseData<List<DailyLogResponse>>> getDailyLogs(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ResponseData.success(myPageService.getDailyLogs(userId, start, end)));
    }

    @PutMapping("/daily-logs/{date}/memo")
    public ResponseEntity<ResponseData<DailyLogResponse>> updateMemo(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody DailyLogMemoRequest request) {
        return ResponseEntity.ok(ResponseData.success(
                myPageService.updateMemo(userId, date, request.getPersonalMemo()),
                "메모가 저장되었습니다."));
    }
}
