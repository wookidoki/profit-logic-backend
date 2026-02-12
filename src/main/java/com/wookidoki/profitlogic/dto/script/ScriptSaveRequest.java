package com.wookidoki.profitlogic.dto.script;

import com.wookidoki.profitlogic.domain.CreatorCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScriptSaveRequest {

    @NotNull(message = "카테고리는 필수입니다.")
    private CreatorCategory category;

    @NotNull(message = "입력값은 필수입니다.")
    private Map<String, Object> inputs;

    private String title;
}
