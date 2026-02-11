package com.wookidoki.profitlogic.dto.script;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptSection {

    private String sectionTitle;
    private List<ScriptField> fields;
}
