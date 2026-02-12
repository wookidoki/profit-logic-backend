package com.wookidoki.profitlogic.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionCardDto {

    private String type;
    private String title;
    private String description;
    private int priority;
}
