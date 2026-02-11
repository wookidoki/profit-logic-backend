package com.wookidoki.profitlogic.dto.script;

import com.wookidoki.profitlogic.domain.CreatorCategory;
import com.wookidoki.profitlogic.dto.CalculateResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptAnalysisResponse {

    private CreatorCategory category;
    private String displayName;
    private Map<String, Object> inputs;
    private CalculateResponse result;

    public static ScriptAnalysisResponse of(CreatorCategory category,
                                             Map<String, Object> inputs,
                                             CalculateResponse result) {
        return ScriptAnalysisResponse.builder()
                .category(category)
                .displayName(category.getDisplayName())
                .inputs(inputs)
                .result(result)
                .build();
    }
}
