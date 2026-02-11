package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ResponseData<Map<String, String>>> health() {
        return ResponseEntity.ok(ResponseData.success(
                Map.of("status", "UP", "service", "Profit Logic Backend")
        ));
    }
}
