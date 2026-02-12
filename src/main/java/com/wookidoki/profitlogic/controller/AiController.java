package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.domain.CreatorCategory;
import com.wookidoki.profitlogic.dto.AiParseRequest;
import com.wookidoki.profitlogic.dto.AiParseResponse;
import com.wookidoki.profitlogic.dto.CalculateRequest;
import com.wookidoki.profitlogic.service.RegexAiParseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final RegexAiParseService aiParseService;

    @PostMapping("/parse")
    public ResponseEntity<ResponseData<AiParseResponse>> parse(
            @Valid @RequestBody AiParseRequest request) {
        CalculateRequest calcResult = aiParseService.parse(request.getText());
        CreatorCategory detected = aiParseService.detectCategory(request.getText());

        String suggestion = null;
        if (detected != null) {
            suggestion = detected.getDisplayName() + "으로 감지되었습니다. 맞춤 분석을 추천합니다.";
        }

        AiParseResponse response = AiParseResponse.from(
                calcResult, detected != null ? detected.name() : null, suggestion);

        return ResponseEntity.ok(ResponseData.success(response));
    }
}
