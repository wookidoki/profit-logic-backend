package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.domain.logic.FinancialCalculator;
import com.wookidoki.profitlogic.dto.CalculateRequest;
import com.wookidoki.profitlogic.dto.CalculateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CalculationService {

    private final FinancialCalculator calculator;

    public CalculateResponse calculate(CalculateRequest request) {
        BigDecimal price = request.getPrice();
        BigDecimal variableCost = request.getVariableCost();
        BigDecimal fixedCost = request.getFixedCost();
        BigDecimal workHours = new BigDecimal(request.getWorkHours());
        BigDecimal hourlyWage = request.getHourlyWage();
        BigDecimal targetProfit = request.getTargetProfit();

        // 공헌이익 (단위당)
        BigDecimal contributionMargin = price.subtract(variableCost);

        // 손익분기점 (CEILING 올림 → 정수)
        BigDecimal bep = calculator.calculateBEP(fixedCost, price, variableCost);

        // 목표 판매량
        BigDecimal targetQuantity = calculator.calculateTargetSales(
                fixedCost, targetProfit, price, variableCost);

        // 영업이익 (목표 판매량 기준)
        BigDecimal operatingProfit = calculator.calculateOperatingProfit(
                price, variableCost, targetQuantity, fixedCost);

        // 경제적 이윤
        BigDecimal economicProfit = calculator.calculateEconomicProfit(
                operatingProfit, workHours, hourlyWage);

        // 안전마진율 (목표 판매량 기준)
        BigDecimal marginRate = calculator.calculateMarginRate(targetQuantity, bep);

        // 실질 시급
        BigDecimal shadowWage = calculator.calculateShadowWage(operatingProfit, workHours);

        // 생존 가능 여부: 경제적 이윤이 양수인지
        boolean isViable = economicProfit.compareTo(BigDecimal.ZERO) > 0;

        return CalculateResponse.builder()
                .breakEvenPoint(bep)
                .operatingProfit(operatingProfit)
                .economicProfit(economicProfit)
                .targetQuantity(targetQuantity)
                .marginRate(marginRate)
                .contributionMargin(contributionMargin)
                .shadowWage(shadowWage)
                .isViable(isViable)
                .build();
    }
}
