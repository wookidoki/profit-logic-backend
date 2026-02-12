package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.BusinessLogicException;
import com.wookidoki.profitlogic.domain.CreatorCategory;
import com.wookidoki.profitlogic.dto.finance.CalculateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ScriptConversionService 단위 테스트")
class ScriptConversionServiceTest {

    private ScriptConversionService conversionService;

    @BeforeEach
    void setUp() {
        conversionService = new ScriptConversionService();
    }

    @Nested
    @DisplayName("WEB_NOVEL 변환")
    class WebNovel {

        @Test
        @DisplayName("웹소설 입력값을 CalculateRequest로 변환한다")
        void shouldConvertWebNovelInputs() {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("episodePrice", 300);
            inputs.put("revenueShareRate", 70);
            inputs.put("episodesPerMonth", 20);
            inputs.put("writingHoursPerEpisode", 4);
            inputs.put("editingHoursPerEpisode", 1);
            inputs.put("monthlyFixedCost", 50000);
            inputs.put("hourlyWage", 9860);
            inputs.put("targetMonthlyIncome", 2000000);

            CalculateRequest result = conversionService.convert(CreatorCategory.WEB_NOVEL, inputs);

            // price = 300 × 70/100 = 210
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("210.00"));
            // variableCost = 300 - 210 = 90
            assertThat(result.getVariableCost()).isEqualByComparingTo(new BigDecimal("90.00"));
            assertThat(result.getFixedCost()).isEqualByComparingTo(new BigDecimal("50000"));
            // workHours = (4 + 1) × 20 = 100
            assertThat(result.getWorkHours()).isEqualTo(100);
            assertThat(result.getHourlyWage()).isEqualByComparingTo(new BigDecimal("9860"));
            assertThat(result.getTargetProfit()).isEqualByComparingTo(new BigDecimal("2000000"));
        }

        @Test
        @DisplayName("퇴고 시간 없이도 변환 가능하다")
        void shouldConvertWithoutEditingHours() {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("episodePrice", 300);
            inputs.put("revenueShareRate", 70);
            inputs.put("episodesPerMonth", 20);
            inputs.put("writingHoursPerEpisode", 4);
            inputs.put("monthlyFixedCost", 0);
            inputs.put("hourlyWage", 9860);
            inputs.put("targetMonthlyIncome", 2000000);

            CalculateRequest result = conversionService.convert(CreatorCategory.WEB_NOVEL, inputs);

            // workHours = (4 + 0) × 20 = 80
            assertThat(result.getWorkHours()).isEqualTo(80);
        }
    }

    @Nested
    @DisplayName("SHORT_FORM 변환")
    class ShortForm {

        @Test
        @DisplayName("숏폼 입력값을 CalculateRequest로 변환한다")
        void shouldConvertShortFormInputs() {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("avgViewsPerVideo", 10000);
            inputs.put("rpmRate", 800);
            inputs.put("videosPerMonth", 20);
            inputs.put("sponsorshipPerVideo", 50000);
            inputs.put("filmingHoursPerVideo", 1);
            inputs.put("editingHoursPerVideo", 2);
            inputs.put("equipmentMonthlyCost", 100000);
            inputs.put("softwareMonthlyCost", 20000);
            inputs.put("hourlyWage", 9860);
            inputs.put("targetMonthlyIncome", 1500000);

            CalculateRequest result = conversionService.convert(CreatorCategory.SHORT_FORM, inputs);

            // price = (10000 × 800 / 1000) + 50000 = 8000 + 50000 = 58000
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("58000.00"));
            assertThat(result.getVariableCost()).isEqualByComparingTo(BigDecimal.ZERO);
            // fixedCost = 100000 + 20000 = 120000
            assertThat(result.getFixedCost()).isEqualByComparingTo(new BigDecimal("120000"));
            // workHours = (1 + 2) × 20 = 60
            assertThat(result.getWorkHours()).isEqualTo(60);
            assertThat(result.getTargetProfit()).isEqualByComparingTo(new BigDecimal("1500000"));
        }

        @Test
        @DisplayName("협찬 없이도 변환 가능하다")
        void shouldConvertWithoutSponsorship() {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("avgViewsPerVideo", 10000);
            inputs.put("rpmRate", 800);
            inputs.put("videosPerMonth", 20);
            inputs.put("filmingHoursPerVideo", 1);
            inputs.put("editingHoursPerVideo", 2);
            inputs.put("hourlyWage", 9860);
            inputs.put("targetMonthlyIncome", 1500000);

            CalculateRequest result = conversionService.convert(CreatorCategory.SHORT_FORM, inputs);

            // price = 10000 × 800 / 1000 = 8000 (no sponsorship)
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("8000.00"));
        }
    }

    @Nested
    @DisplayName("EMOTICON 변환")
    class Emoticon {

        @Test
        @DisplayName("이모티콘 입력값을 CalculateRequest로 변환한다")
        void shouldConvertEmoticonInputs() {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("setPrice", 2500);
            inputs.put("revenueShareRate", 33);
            inputs.put("tabletMonthlyCost", 30000);
            inputs.put("softwareMonthlyCost", 11000);
            inputs.put("monthlyMaintenanceHours", 10);
            inputs.put("hourlyWage", 9860);
            inputs.put("targetMonthlyIncome", 1000000);

            CalculateRequest result = conversionService.convert(CreatorCategory.EMOTICON, inputs);

            // price = 2500 × 33/100 = 825
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("825.00"));
            // variableCost = 2500 - 825 = 1675
            assertThat(result.getVariableCost()).isEqualByComparingTo(new BigDecimal("1675.00"));
            // fixedCost = 30000 + 11000 = 41000
            assertThat(result.getFixedCost()).isEqualByComparingTo(new BigDecimal("41000"));
            assertThat(result.getWorkHours()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("BLOG 변환")
    class Blog {

        @Test
        @DisplayName("블로그 입력값을 CalculateRequest로 변환한다")
        void shouldConvertBlogInputs() {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("monthlyPageViews", 50000);
            inputs.put("rpmRate", 1500);
            inputs.put("postsPerMonth", 15);
            inputs.put("affiliateIncomePerMonth", 200000);
            inputs.put("writingHoursPerPost", 3);
            inputs.put("seoManagementHours", 5);
            inputs.put("hostingMonthlyCost", 10000);
            inputs.put("toolsMonthlyCost", 20000);
            inputs.put("hourlyWage", 9860);
            inputs.put("targetMonthlyIncome", 1000000);

            CalculateRequest result = conversionService.convert(CreatorCategory.BLOG, inputs);

            // 월 광고 수익 = 50000 × 1500 / 1000 = 75000
            // 게시물당 수익 = (75000 + 200000) / 15 = 18333.33
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("18333.33"));
            assertThat(result.getVariableCost()).isEqualByComparingTo(BigDecimal.ZERO);
            // fixedCost = 10000 + 20000 = 30000
            assertThat(result.getFixedCost()).isEqualByComparingTo(new BigDecimal("30000"));
            // workHours = (3 × 15) + 5 = 50
            assertThat(result.getWorkHours()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("INDIE_DEV 변환")
    class IndieDev {

        @Test
        @DisplayName("인디 개발자 입력값을 CalculateRequest로 변환한다")
        void shouldConvertIndieDevInputs() {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("unitPrice", 9900);
            inputs.put("platformFeeRate", 10);
            inputs.put("serverMonthlyCost", 50000);
            inputs.put("toolsMonthlyCost", 30000);
            inputs.put("developmentHoursPerMonth", 80);
            inputs.put("supportHoursPerMonth", 10);
            inputs.put("marketingHoursPerMonth", 10);
            inputs.put("hourlyWage", 15000);
            inputs.put("targetMonthlyIncome", 3000000);

            CalculateRequest result = conversionService.convert(CreatorCategory.INDIE_DEV, inputs);

            // feePerUnit = 9900 × 10/100 = 990
            // price = 9900 - 990 = 8910
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("8910.00"));
            assertThat(result.getVariableCost()).isEqualByComparingTo(new BigDecimal("990.00"));
            // fixedCost = 50000 + 30000 = 80000
            assertThat(result.getFixedCost()).isEqualByComparingTo(new BigDecimal("80000"));
            // workHours = 80 + 10 + 10 = 100
            assertThat(result.getWorkHours()).isEqualTo(100);
            assertThat(result.getHourlyWage()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(result.getTargetProfit()).isEqualByComparingTo(new BigDecimal("3000000"));
        }
    }

    @Nested
    @DisplayName("에러 처리")
    class ErrorHandling {

        @Test
        @DisplayName("필수 필드가 누락되면 BusinessLogicException이 발생한다")
        void shouldThrowWhenRequiredFieldMissing() {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("episodePrice", 300);
            // revenueShareRate 누락

            assertThatThrownBy(() ->
                    conversionService.convert(CreatorCategory.WEB_NOVEL, inputs))
                    .isInstanceOf(BusinessLogicException.class)
                    .hasMessageContaining("필수 입력값이 누락");
        }

        @Test
        @DisplayName("숫자가 아닌 값이 입력되면 BusinessLogicException이 발생한다")
        void shouldThrowWhenInvalidNumberFormat() {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("episodePrice", "abc");
            inputs.put("revenueShareRate", 70);
            inputs.put("episodesPerMonth", 20);
            inputs.put("writingHoursPerEpisode", 4);
            inputs.put("monthlyFixedCost", 0);
            inputs.put("hourlyWage", 9860);
            inputs.put("targetMonthlyIncome", 2000000);

            assertThatThrownBy(() ->
                    conversionService.convert(CreatorCategory.WEB_NOVEL, inputs))
                    .isInstanceOf(BusinessLogicException.class)
                    .hasMessageContaining("숫자 형식이 올바르지 않습니다");
        }

        @Test
        @DisplayName("String 형태의 숫자도 변환 가능하다")
        void shouldAcceptStringNumbers() {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("episodePrice", "300");
            inputs.put("revenueShareRate", "70");
            inputs.put("episodesPerMonth", "20");
            inputs.put("writingHoursPerEpisode", "4");
            inputs.put("monthlyFixedCost", "0");
            inputs.put("hourlyWage", "9860");
            inputs.put("targetMonthlyIncome", "2000000");

            CalculateRequest result = conversionService.convert(CreatorCategory.WEB_NOVEL, inputs);

            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("210.00"));
        }

        @Test
        @DisplayName("BigDecimal 입력도 처리 가능하다")
        void shouldAcceptBigDecimalInputs() {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("episodePrice", new BigDecimal("300"));
            inputs.put("revenueShareRate", new BigDecimal("70"));
            inputs.put("episodesPerMonth", new BigDecimal("20"));
            inputs.put("writingHoursPerEpisode", new BigDecimal("4"));
            inputs.put("monthlyFixedCost", BigDecimal.ZERO);
            inputs.put("hourlyWage", new BigDecimal("9860"));
            inputs.put("targetMonthlyIncome", new BigDecimal("2000000"));

            CalculateRequest result = conversionService.convert(CreatorCategory.WEB_NOVEL, inputs);

            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("210.00"));
        }
    }
}
