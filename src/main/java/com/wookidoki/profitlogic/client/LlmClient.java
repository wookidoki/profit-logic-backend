package com.wookidoki.profitlogic.client;

public interface LlmClient {

    String chat(String systemPrompt, String userMessage);

    String chatHeavy(String systemPrompt, String userMessage);

    LlmResponse chatWithUsage(String systemPrompt, String userMessage);

    boolean isAvailable();
}
