package com.wookidoki.profitlogic.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class LlmResponse {

    private final String content;
    private final int inputTokens;
    private final int outputTokens;
    private final int totalTokens;
}
