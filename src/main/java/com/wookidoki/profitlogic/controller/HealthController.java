package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.client.LlmClient;
import com.wookidoki.profitlogic.common.ResponseData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final LlmClient llmClient;

    @GetMapping("/health")
    public ResponseEntity<ResponseData<Map<String, Object>>> health() {
        return ResponseEntity.ok(ResponseData.success(
                Map.of(
                        "status", "UP",
                        "service", "Profit Logic Backend",
                        "llm_available", llmClient.isAvailable()
                )
        ));
    }
}
