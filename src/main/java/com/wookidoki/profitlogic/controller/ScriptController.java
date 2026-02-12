package com.wookidoki.profitlogic.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.domain.CreatorCategory;
import com.wookidoki.profitlogic.dto.CalculateRequest;
import com.wookidoki.profitlogic.dto.CalculateResponse;
import com.wookidoki.profitlogic.dto.ProjectCreateRequest;
import com.wookidoki.profitlogic.dto.ProjectResponse;
import com.wookidoki.profitlogic.dto.script.CategoryInfo;
import com.wookidoki.profitlogic.dto.script.ScriptAnalysisRequest;
import com.wookidoki.profitlogic.dto.script.ScriptAnalysisResponse;
import com.wookidoki.profitlogic.dto.script.ScriptSaveRequest;
import com.wookidoki.profitlogic.dto.script.ScriptTemplate;
import com.wookidoki.profitlogic.service.CalculationService;
import com.wookidoki.profitlogic.service.ProjectService;
import com.wookidoki.profitlogic.service.ScriptConversionService;
import com.wookidoki.profitlogic.service.ScriptTemplateProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/scripts")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptTemplateProvider templateProvider;
    private final ScriptConversionService conversionService;
    private final CalculationService calculationService;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

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

    /**
     * 스크립트 분석 결과를 프로젝트로 저장한다. (인증 필수)
     */
    @PostMapping("/save-project")
    public ResponseEntity<ResponseData<ProjectResponse>> saveAsProject(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ScriptSaveRequest request) {
        // 카테고리 입력값 → 공통 CalculateRequest 변환
        CalculateRequest calcRequest = conversionService.convert(
                request.getCategory(), request.getInputs());

        // 제목 자동 생성 (미지정 시)
        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            CategoryInfo catInfo = CategoryInfo.from(request.getCategory());
            String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            title = catInfo.getDisplayName() + " 프로젝트 (" + month + ")";
        }

        // 입력값 JSON 직렬화
        String scriptInputsJson;
        try {
            scriptInputsJson = objectMapper.writeValueAsString(request.getInputs());
        } catch (JsonProcessingException e) {
            scriptInputsJson = null;
        }

        // 프로젝트 생성
        ProjectCreateRequest createRequest = ProjectCreateRequest.builder()
                .title(title)
                .price(calcRequest.getPrice())
                .variableCost(calcRequest.getVariableCost())
                .fixedCost(calcRequest.getFixedCost())
                .workHours(calcRequest.getWorkHours())
                .hourlyWage(calcRequest.getHourlyWage())
                .creatorCategory(request.getCategory())
                .scriptInputs(scriptInputsJson)
                .build();

        ProjectResponse response = projectService.create(userId, createRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseData.success(response));
    }
}
