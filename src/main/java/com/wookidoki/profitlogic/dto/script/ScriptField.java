package com.wookidoki.profitlogic.dto.script;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptField {

    private String fieldKey;
    private String label;
    private String fieldType;
    private Object defaultValue;
    private String placeholder;
    private String unit;
    private List<String> options;
    private boolean required;
    private BigDecimal min;
    private BigDecimal max;
    private String helpText;
    private boolean autoCalculate;
    private String formula;
}
