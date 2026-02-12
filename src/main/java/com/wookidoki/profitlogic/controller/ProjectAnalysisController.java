package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.finance.PriceSimulationDto;
import com.wookidoki.profitlogic.dto.project.ProjectAnalysisResponse;
import com.wookidoki.profitlogic.service.ProjectAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/projects/{projectId}/analysis")
@RequiredArgsConstructor
public class ProjectAnalysisController {

    private final ProjectAnalysisService projectAnalysisService;

    @GetMapping
    public ResponseEntity<ResponseData<ProjectAnalysisResponse>> analyze(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId) {
        ProjectAnalysisResponse result = projectAnalysisService.analyzeProject(projectId, userId);
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @GetMapping("/price-simulation")
    public ResponseEntity<ResponseData<PriceSimulationDto>> simulatePrice(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId,
            @RequestParam BigDecimal newPrice,
            @RequestParam(required = false) BigDecimal elasticity) {
        PriceSimulationDto result = projectAnalysisService
                .simulatePriceChange(projectId, userId, newPrice, elasticity);
        return ResponseEntity.ok(ResponseData.success(result));
    }
}
