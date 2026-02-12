package com.wookidoki.profitlogic.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiParseRequest {

    @NotBlank(message = "텍스트는 필수입니다.")
    private String text;
}
