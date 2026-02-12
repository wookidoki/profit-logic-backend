package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.BusinessLogicException;
import com.wookidoki.profitlogic.domain.CreatorCategory;
import com.wookidoki.profitlogic.dto.finance.CalculateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 카테고리별 스크립트 입력값을 공통 CalculateRequest로 변환한다.
 *
 * <p>각 카테고리의 입력 필드는 ScriptTemplateProvider에 정의된 키와 일치해야 한다.</p>
 *
 * <h3>변환 매핑 규칙</h3>
 * <ul>
 *   <li><b>price</b> — 단위당 실질 수익 (판매가 × 정산비율)</li>
 *   <li><b>variableCost</b> — 단위당 변동비 (플랫폼 수수료 등)</li>
 *   <li><b>fixedCost</b> — 월 고정비 합계</li>
 *   <li><b>workHours</b> — 월 총 작업 시간</li>
 *   <li><b>hourlyWage</b> — 기회비용 시급</li>
 *   <li><b>targetProfit</b> — 월 목표 수입</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ScriptConversionService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    /**
     * 카테고리에 맞는 변환 로직을 적용하여 CalculateRequest를 생성한다.
     */
    public CalculateRequest convert(CreatorCategory category, Map<String, Object> inputs) {
        return switch (category) {
            case WEB_NOVEL -> convertWebNovel(inputs);
            case SHORT_FORM -> convertShortForm(inputs);
            case EMOTICON -> convertEmoticon(inputs);
            case BLOG -> convertBlog(inputs);
            case INDIE_DEV -> convertIndieDev(inputs);
        };
    }

    /**
     * 웹소설 작가
     * <ul>
     *   <li>price = episodePrice × (revenueShareRate / 100) — 회당 실질 수익</li>
     *   <li>variableCost = episodePrice × ((100 - revenueShareRate) / 100) — 회당 플랫폼 수수료</li>
     *   <li>fixedCost = monthlyFixedCost</li>
     *   <li>workHours = (writingHours + editingHours) × episodesPerMonth</li>
     *   <li>hourlyWage = hourlyWage</li>
     *   <li>targetProfit = targetMonthlyIncome</li>
     * </ul>
     * <p>BEP 의미: 손익분기에 필요한 유료 열람 횟수 (전체, 회차×독자 합산)</p>
     */
    private CalculateRequest convertWebNovel(Map<String, Object> inputs) {
        BigDecimal episodePrice = getRequiredDecimal(inputs, "episodePrice");
        BigDecimal revenueShareRate = getRequiredDecimal(inputs, "revenueShareRate");
        BigDecimal episodesPerMonth = getRequiredDecimal(inputs, "episodesPerMonth");
        BigDecimal writingHours = getRequiredDecimal(inputs, "writingHoursPerEpisode");
        BigDecimal editingHours = getDecimalOrDefault(inputs, "editingHoursPerEpisode", BigDecimal.ZERO);
        BigDecimal monthlyFixedCost = getRequiredDecimal(inputs, "monthlyFixedCost");
        BigDecimal hourlyWage = getRequiredDecimal(inputs, "hourlyWage");
        BigDecimal targetMonthlyIncome = getRequiredDecimal(inputs, "targetMonthlyIncome");

        // 회당 실질 수익 = 판매가 × 정산비율
        BigDecimal price = episodePrice.multiply(revenueShareRate)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        // 회당 변동비 = 판매가 - 실질 수익 (= 플랫폼 수수료)
        BigDecimal variableCost = episodePrice.subtract(price);

        // 월 총 작업 시간
        int workHours = writingHours.add(editingHours)
                .multiply(episodesPerMonth)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        return CalculateRequest.builder()
                .price(price)
                .variableCost(variableCost)
                .fixedCost(monthlyFixedCost)
                .workHours(workHours)
                .hourlyWage(hourlyWage)
                .targetProfit(targetMonthlyIncome)
                .build();
    }

    /**
     * 숏폼 크리에이터
     * <ul>
     *   <li>price = (avgViews × rpmRate / 1000) + sponsorship — 영상 1개당 수익</li>
     *   <li>variableCost = 0 (플랫폼 수수료 없음, 조회 기반 수익)</li>
     *   <li>fixedCost = equipmentMonthlyCost + softwareMonthlyCost</li>
     *   <li>workHours = (filmingHours + editingHours) × videosPerMonth</li>
     *   <li>hourlyWage = hourlyWage</li>
     *   <li>targetProfit = targetMonthlyIncome</li>
     * </ul>
     * <p>BEP 의미: 손익분기에 필요한 월 영상 수</p>
     */
    private CalculateRequest convertShortForm(Map<String, Object> inputs) {
        BigDecimal avgViews = getRequiredDecimal(inputs, "avgViewsPerVideo");
        BigDecimal rpmRate = getRequiredDecimal(inputs, "rpmRate");
        BigDecimal videosPerMonth = getRequiredDecimal(inputs, "videosPerMonth");
        BigDecimal sponsorship = getDecimalOrDefault(inputs, "sponsorshipPerVideo", BigDecimal.ZERO);
        BigDecimal filmingHours = getRequiredDecimal(inputs, "filmingHoursPerVideo");
        BigDecimal editingHours = getRequiredDecimal(inputs, "editingHoursPerVideo");
        BigDecimal equipmentCost = getDecimalOrDefault(inputs, "equipmentMonthlyCost", BigDecimal.ZERO);
        BigDecimal softwareCost = getDecimalOrDefault(inputs, "softwareMonthlyCost", BigDecimal.ZERO);
        BigDecimal hourlyWage = getRequiredDecimal(inputs, "hourlyWage");
        BigDecimal targetMonthlyIncome = getRequiredDecimal(inputs, "targetMonthlyIncome");

        // 영상 1개당 수익 = (조회수 × RPM / 1000) + 협찬
        BigDecimal adRevenue = avgViews.multiply(rpmRate)
                .divide(THOUSAND, 2, RoundingMode.HALF_UP);
        BigDecimal price = adRevenue.add(sponsorship);

        BigDecimal fixedCost = equipmentCost.add(softwareCost);

        int workHours = filmingHours.add(editingHours)
                .multiply(videosPerMonth)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        return CalculateRequest.builder()
                .price(price)
                .variableCost(BigDecimal.ZERO)
                .fixedCost(fixedCost)
                .workHours(workHours)
                .hourlyWage(hourlyWage)
                .targetProfit(targetMonthlyIncome)
                .build();
    }

    /**
     * 이모티콘 셀러
     * <ul>
     *   <li>price = setPrice × (revenueShareRate / 100) — 세트당 실질 수익</li>
     *   <li>variableCost = setPrice - price — 세트당 플랫폼 수수료</li>
     *   <li>fixedCost = tabletMonthlyCost + softwareMonthlyCost</li>
     *   <li>workHours = monthlyMaintenanceHours (유지 시간; 제작 시간은 초기 투입이므로 별도)</li>
     *   <li>hourlyWage = hourlyWage</li>
     *   <li>targetProfit = targetMonthlyIncome</li>
     * </ul>
     * <p>BEP 의미: 손익분기에 필요한 월 세트 판매 수</p>
     */
    private CalculateRequest convertEmoticon(Map<String, Object> inputs) {
        BigDecimal setPrice = getRequiredDecimal(inputs, "setPrice");
        BigDecimal revenueShareRate = getRequiredDecimal(inputs, "revenueShareRate");
        BigDecimal tabletCost = getDecimalOrDefault(inputs, "tabletMonthlyCost", BigDecimal.ZERO);
        BigDecimal softwareCost = getDecimalOrDefault(inputs, "softwareMonthlyCost", BigDecimal.ZERO);
        BigDecimal maintenanceHours = getDecimalOrDefault(inputs, "monthlyMaintenanceHours", BigDecimal.TEN);
        BigDecimal hourlyWage = getRequiredDecimal(inputs, "hourlyWage");
        BigDecimal targetMonthlyIncome = getRequiredDecimal(inputs, "targetMonthlyIncome");

        BigDecimal price = setPrice.multiply(revenueShareRate)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal variableCost = setPrice.subtract(price);
        BigDecimal fixedCost = tabletCost.add(softwareCost);

        int workHours = maintenanceHours.setScale(0, RoundingMode.HALF_UP).intValue();

        return CalculateRequest.builder()
                .price(price)
                .variableCost(variableCost)
                .fixedCost(fixedCost)
                .workHours(workHours)
                .hourlyWage(hourlyWage)
                .targetProfit(targetMonthlyIncome)
                .build();
    }

    /**
     * 블로그/뉴스레터
     * <ul>
     *   <li>price = (monthlyPageViews × rpmRate / 1000 + affiliateIncome) / postsPerMonth — 게시물 1개당 수익</li>
     *   <li>variableCost = 0</li>
     *   <li>fixedCost = hostingMonthlyCost + toolsMonthlyCost</li>
     *   <li>workHours = (writingHoursPerPost × postsPerMonth) + seoManagementHours</li>
     *   <li>hourlyWage = hourlyWage</li>
     *   <li>targetProfit = targetMonthlyIncome</li>
     * </ul>
     * <p>BEP 의미: 손익분기에 필요한 월 게시물 수</p>
     */
    private CalculateRequest convertBlog(Map<String, Object> inputs) {
        BigDecimal monthlyPV = getRequiredDecimal(inputs, "monthlyPageViews");
        BigDecimal rpmRate = getRequiredDecimal(inputs, "rpmRate");
        BigDecimal postsPerMonth = getRequiredDecimal(inputs, "postsPerMonth");
        BigDecimal affiliateIncome = getDecimalOrDefault(inputs, "affiliateIncomePerMonth", BigDecimal.ZERO);
        BigDecimal writingHours = getRequiredDecimal(inputs, "writingHoursPerPost");
        BigDecimal seoHours = getDecimalOrDefault(inputs, "seoManagementHours", BigDecimal.ZERO);
        BigDecimal hostingCost = getDecimalOrDefault(inputs, "hostingMonthlyCost", BigDecimal.ZERO);
        BigDecimal toolsCost = getDecimalOrDefault(inputs, "toolsMonthlyCost", BigDecimal.ZERO);
        BigDecimal hourlyWage = getRequiredDecimal(inputs, "hourlyWage");
        BigDecimal targetMonthlyIncome = getRequiredDecimal(inputs, "targetMonthlyIncome");

        // 월 광고 수익
        BigDecimal monthlyAdRevenue = monthlyPV.multiply(rpmRate)
                .divide(THOUSAND, 2, RoundingMode.HALF_UP);

        // 게시물 1개당 수익 = (광고 수익 + 제휴 수익) / 월 게시물 수
        BigDecimal totalMonthlyRevenue = monthlyAdRevenue.add(affiliateIncome);
        BigDecimal price = totalMonthlyRevenue.divide(postsPerMonth, 2, RoundingMode.HALF_UP);

        BigDecimal fixedCost = hostingCost.add(toolsCost);

        int workHours = writingHours.multiply(postsPerMonth)
                .add(seoHours)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        return CalculateRequest.builder()
                .price(price)
                .variableCost(BigDecimal.ZERO)
                .fixedCost(fixedCost)
                .workHours(workHours)
                .hourlyWage(hourlyWage)
                .targetProfit(targetMonthlyIncome)
                .build();
    }

    /**
     * 인디 개발자/SaaS
     * <ul>
     *   <li>price = unitPrice × ((100 - platformFeeRate) / 100) — 건당 실질 수익</li>
     *   <li>variableCost = unitPrice × (platformFeeRate / 100) — 건당 수수료</li>
     *   <li>fixedCost = serverMonthlyCost + toolsMonthlyCost</li>
     *   <li>workHours = devHours + supportHours + marketingHours</li>
     *   <li>hourlyWage = hourlyWage</li>
     *   <li>targetProfit = targetMonthlyIncome</li>
     * </ul>
     * <p>BEP 의미: 손익분기에 필요한 월 구독자/판매 건수</p>
     */
    private CalculateRequest convertIndieDev(Map<String, Object> inputs) {
        BigDecimal unitPrice = getRequiredDecimal(inputs, "unitPrice");
        BigDecimal platformFeeRate = getRequiredDecimal(inputs, "platformFeeRate");
        BigDecimal serverCost = getRequiredDecimal(inputs, "serverMonthlyCost");
        BigDecimal toolsCost = getDecimalOrDefault(inputs, "toolsMonthlyCost", BigDecimal.ZERO);
        BigDecimal devHours = getRequiredDecimal(inputs, "developmentHoursPerMonth");
        BigDecimal supportHours = getDecimalOrDefault(inputs, "supportHoursPerMonth", BigDecimal.ZERO);
        BigDecimal marketingHours = getDecimalOrDefault(inputs, "marketingHoursPerMonth", BigDecimal.ZERO);
        BigDecimal hourlyWage = getRequiredDecimal(inputs, "hourlyWage");
        BigDecimal targetMonthlyIncome = getRequiredDecimal(inputs, "targetMonthlyIncome");

        // 건당 수수료
        BigDecimal feePerUnit = unitPrice.multiply(platformFeeRate)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        // 건당 실질 수익
        BigDecimal price = unitPrice.subtract(feePerUnit);
        BigDecimal variableCost = feePerUnit;

        BigDecimal fixedCost = serverCost.add(toolsCost);

        int workHours = devHours.add(supportHours).add(marketingHours)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        return CalculateRequest.builder()
                .price(price)
                .variableCost(variableCost)
                .fixedCost(fixedCost)
                .workHours(workHours)
                .hourlyWage(hourlyWage)
                .targetProfit(targetMonthlyIncome)
                .build();
    }

    // ── Helper methods ──────────────────────────────────

    private BigDecimal getRequiredDecimal(Map<String, Object> inputs, String key) {
        Object value = inputs.get(key);
        if (value == null) {
            throw new BusinessLogicException("필수 입력값이 누락되었습니다: " + key);
        }
        return toBigDecimal(value, key);
    }

    private BigDecimal getDecimalOrDefault(Map<String, Object> inputs, String key, BigDecimal defaultValue) {
        Object value = inputs.get(key);
        if (value == null) {
            return defaultValue;
        }
        return toBigDecimal(value, key);
    }

    private BigDecimal toBigDecimal(Object value, String key) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String str) {
            try {
                return new BigDecimal(str);
            } catch (NumberFormatException e) {
                throw new BusinessLogicException("숫자 형식이 올바르지 않습니다: " + key + " = " + str);
            }
        }
        throw new BusinessLogicException("지원하지 않는 입력값 형식입니다: " + key);
    }
}
