package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.chat.ChatRequest;
import com.wookidoki.profitlogic.dto.chat.ChatResponse;
import com.wookidoki.profitlogic.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ResponseData<ChatResponse>> chat(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChatRequest request) {
        ChatResponse result = chatService.chat(userId, request);
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @GetMapping("/history/{projectId}")
    public ResponseEntity<ResponseData<List<ChatResponse>>> getHistory(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId) {
        List<ChatResponse> result = chatService.getHistory(userId, projectId);
        return ResponseEntity.ok(ResponseData.success(result));
    }
}
