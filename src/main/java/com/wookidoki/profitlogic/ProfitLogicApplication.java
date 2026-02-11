package com.wookidoki.profitlogic;

import com.wookidoki.profitlogic.config.LlmProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LlmProperties.class)
public class ProfitLogicApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfitLogicApplication.class, args);
    }
}
