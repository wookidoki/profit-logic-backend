package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.AiParseRequest;
import com.wookidoki.profitlogic.dto.CalculateRequest;
import com.wookidoki.profitlogic.service.AiParseService;
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

    private final AiParseService aiParseService;

    @PostMapping("/parse")
    public ResponseEntity<ResponseData<CalculateRequest>> parse(
            @Valid @RequestBody AiParseRequest request) {
        CalculateRequest result = aiParseService.parse(request.getText());
        return ResponseEntity.ok(ResponseData.success(result));
    }
}
