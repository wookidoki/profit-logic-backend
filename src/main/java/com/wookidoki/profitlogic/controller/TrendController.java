package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.trend.MonthlyTrendResponse;
import com.wookidoki.profitlogic.service.TrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/projects/{projectId}/trends")
@RequiredArgsConstructor
public class TrendController {

    private final TrendService trendService;

    @GetMapping
    public ResponseEntity<ResponseData<MonthlyTrendResponse>> getMonthlyTrends(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "6") int months) {
        MonthlyTrendResponse result = trendService.getMonthlyTrends(projectId, userId, months);
        return ResponseEntity.ok(ResponseData.success(result));
    }
}
