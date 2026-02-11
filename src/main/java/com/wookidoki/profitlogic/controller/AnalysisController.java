package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.CalculateRequest;
import com.wookidoki.profitlogic.dto.CalculateResponse;
import com.wookidoki.profitlogic.service.CalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final CalculationService calculationService;

    @PostMapping("/calculate")
    public ResponseEntity<ResponseData<CalculateResponse>> calculate(
            @Valid @RequestBody CalculateRequest request) {
        CalculateResponse result = calculationService.calculate(request);
        return ResponseEntity.ok(ResponseData.success(result));
    }
}
