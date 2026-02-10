package com.wookidoki.profitlogic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateResponse {

    /** 손익분기점 (판매 수량) */
    private BigDecimal breakEvenPoint;

    /** 영업이익 (목표 판매량 기준) */
    private BigDecimal operatingProfit;

    /** 경제적 이윤 (기회비용 차감 후) */
    private BigDecimal economicProfit;

    /** 목표 판매량 */
    private BigDecimal targetQuantity;

    /** 안전마진율 (%) */
    private BigDecimal marginRate;

    /** 공헌이익 (단위당) */
    private BigDecimal contributionMargin;

    /** 생존 가능 여부 (경제적 이윤 > 0) */
    private Boolean isViable;
}
