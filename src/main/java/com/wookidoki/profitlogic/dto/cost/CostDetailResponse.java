package com.wookidoki.profitlogic.dto.cost;

import com.wookidoki.profitlogic.domain.CostCategory;
import com.wookidoki.profitlogic.domain.CostDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostDetailResponse {

    private Long id;
    private Long projectId;
    private CostCategory category;
    private String costName;
    private String costType;
    private BigDecimal amount;
    private String memo;
    private LocalDateTime createdAt;

    public static CostDetailResponse from(CostDetail costDetail) {
        return CostDetailResponse.builder()
                .id(costDetail.getId())
                .projectId(costDetail.getProject().getId())
                .category(costDetail.getCategory())
                .costName(costDetail.getCostName())
                .costType(costDetail.getCostType())
                .amount(costDetail.getAmount())
                .memo(costDetail.getMemo())
                .createdAt(costDetail.getCreatedAt())
                .build();
    }
}
