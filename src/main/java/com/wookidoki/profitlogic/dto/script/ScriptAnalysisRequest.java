package com.wookidoki.profitlogic.dto.script;

import com.wookidoki.profitlogic.domain.CreatorCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptAnalysisRequest {

    @NotNull(message = "카테고리는 필수입니다.")
    private CreatorCategory category;

    @NotNull(message = "입력 데이터는 필수입니다.")
    private Map<String, Object> inputs;
}
