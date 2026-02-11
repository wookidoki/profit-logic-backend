package com.wookidoki.profitlogic.dto;

import com.wookidoki.profitlogic.domain.ChatLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    private Long id;
    private String question;
    private String answer;
    private Integer tokensUsed;
    private LocalDateTime createdAt;

    public static ChatResponse from(ChatLog chatLog) {
        return ChatResponse.builder()
                .id(chatLog.getId())
                .question(chatLog.getQuestion())
                .answer(chatLog.getAnswer())
                .tokensUsed(chatLog.getTokensUsed())
                .createdAt(chatLog.getCreatedAt())
                .build();
    }
}
