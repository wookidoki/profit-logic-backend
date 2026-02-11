package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.SimulationCreateRequest;
import com.wookidoki.profitlogic.dto.SimulationResponse;
import com.wookidoki.profitlogic.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping
    public ResponseEntity<ResponseData<SimulationResponse>> save(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SimulationCreateRequest request) {
        SimulationResponse result = simulationService.save(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseData.success(result, "시뮬레이션이 저장되었습니다."));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ResponseData<List<SimulationResponse>>> getByProject(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId) {
        List<SimulationResponse> result = simulationService.getByProject(userId, projectId);
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @DeleteMapping("/{simulationId}")
    public ResponseEntity<ResponseData<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long simulationId) {
        simulationService.delete(simulationId, userId);
        return ResponseEntity.ok(ResponseData.success(null, "시뮬레이션이 삭제되었습니다."));
    }
}
