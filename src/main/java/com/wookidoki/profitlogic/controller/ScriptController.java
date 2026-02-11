package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.domain.CreatorCategory;
import com.wookidoki.profitlogic.dto.CalculateRequest;
import com.wookidoki.profitlogic.dto.CalculateResponse;
import com.wookidoki.profitlogic.dto.script.CategoryInfo;
import com.wookidoki.profitlogic.dto.script.ScriptAnalysisRequest;
import com.wookidoki.profitlogic.dto.script.ScriptAnalysisResponse;
import com.wookidoki.profitlogic.dto.script.ScriptTemplate;
import com.wookidoki.profitlogic.service.CalculationService;
import com.wookidoki.profitlogic.service.ScriptConversionService;
import com.wookidoki.profitlogic.service.ScriptTemplateProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/scripts")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptTemplateProvider templateProvider;
    private final ScriptConversionService conversionService;
    private final CalculationService calculationService;

    /**
     * 지원하는 크리에이터 카테고리 목록을 반환한다.
     */
    @GetMapping("/categories")
    public ResponseEntity<ResponseData<List<CategoryInfo>>> getCategories() {
        List<CategoryInfo> categories = Arrays.stream(CreatorCategory.values())
                .map(CategoryInfo::from)
                .toList();
        return ResponseEntity.ok(ResponseData.success(categories));
    }

    /**
     * 특정 카테고리의 스크립트 템플릿(폼 정의)을 반환한다.
     */
    @GetMapping("/templates/{category}")
    public ResponseEntity<ResponseData<ScriptTemplate>> getTemplate(
            @PathVariable CreatorCategory category) {
        ScriptTemplate template = templateProvider.getTemplate(category);
        return ResponseEntity.ok(ResponseData.success(template));
    }

    /**
     * 카테고리별 입력값을 받아 재무 분석을 수행한다.
     */
    @PostMapping("/analyze")
    public ResponseEntity<ResponseData<ScriptAnalysisResponse>> analyze(
            @Valid @RequestBody ScriptAnalysisRequest request) {
        CalculateRequest calcRequest = conversionService.convert(
                request.getCategory(), request.getInputs());
        CalculateResponse calcResult = calculationService.calculate(calcRequest);

        ScriptAnalysisResponse response = ScriptAnalysisResponse.of(
                request.getCategory(), request.getInputs(), calcResult);
        return ResponseEntity.ok(ResponseData.success(response));
    }
}
