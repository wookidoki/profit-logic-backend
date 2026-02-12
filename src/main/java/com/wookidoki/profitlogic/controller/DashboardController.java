package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.DashboardSummaryResponse;
import com.wookidoki.profitlogic.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ResponseData<DashboardSummaryResponse>> getSummary(
            @AuthenticationPrincipal Long userId) {
        DashboardSummaryResponse result = dashboardService.getSummary(userId);
        return ResponseEntity.ok(ResponseData.success(result));
    }
}
