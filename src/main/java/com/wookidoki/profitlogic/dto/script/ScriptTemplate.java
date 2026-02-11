package com.wookidoki.profitlogic.dto.script;

import com.wookidoki.profitlogic.domain.CreatorCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptTemplate {

    private CreatorCategory category;
    private String displayName;
    private String description;
    private List<ScriptSection> sections;
}
