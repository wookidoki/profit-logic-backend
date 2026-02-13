package com.wookidoki.profitlogic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final LlmProperties llmProperties;

    @Bean
    public WebClient geminiWebClient(ObjectMapper baseObjectMapper) {
        // Gemini API는 camelCase를 사용하므로, 글로벌 SNAKE_CASE와 별도의 ObjectMapper 생성
        ObjectMapper geminiMapper = baseObjectMapper.copy()
                .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(conf -> {
                    conf.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(geminiMapper));
                    conf.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(geminiMapper));
                })
                .build();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(30));

        return WebClient.builder()
                .baseUrl(llmProperties.getGemini().getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}
