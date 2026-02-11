package com.wookidoki.profitlogic.dto.script;

import com.wookidoki.profitlogic.domain.CreatorCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryInfo {

    private CreatorCategory category;
    private String displayName;
    private String description;

    public static CategoryInfo from(CreatorCategory category) {
        return CategoryInfo.builder()
                .category(category)
                .displayName(category.getDisplayName())
                .description(category.getDescription())
                .build();
    }
}
