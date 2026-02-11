package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.domain.CreatorCategory;
import com.wookidoki.profitlogic.dto.script.ScriptField;
import com.wookidoki.profitlogic.dto.script.ScriptSection;
import com.wookidoki.profitlogic.dto.script.ScriptTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScriptTemplateProvider 단위 테스트")
class ScriptTemplateProviderTest {

    private ScriptTemplateProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ScriptTemplateProvider();
    }

    @Nested
    @DisplayName("모든 카테고리 공통 검증")
    class AllCategories {

        @ParameterizedTest
        @EnumSource(CreatorCategory.class)
        @DisplayName("모든 카테고리에 대해 템플릿이 존재한다")
        void shouldReturnTemplateForEveryCategory(CreatorCategory category) {
            ScriptTemplate template = provider.getTemplate(category);

            assertThat(template).isNotNull();
            assertThat(template.getCategory()).isEqualTo(category);
            assertThat(template.getDisplayName()).isEqualTo(category.getDisplayName());
            assertThat(template.getDescription()).isEqualTo(category.getDescription());
        }

        @ParameterizedTest
        @EnumSource(CreatorCategory.class)
        @DisplayName("모든 템플릿은 4개 섹션을 가진다 (수익 구조, 시간 투입, 비용, 목표)")
        void shouldHaveFourSections(CreatorCategory category) {
            ScriptTemplate template = provider.getTemplate(category);

            assertThat(template.getSections()).hasSize(4);

            List<String> sectionTitles = template.getSections().stream()
                    .map(ScriptSection::getSectionTitle)
                    .toList();
            assertThat(sectionTitles).containsExactly("수익 구조", "시간 투입", "비용", "목표");
        }

        @ParameterizedTest
        @EnumSource(CreatorCategory.class)
        @DisplayName("모든 필드에 fieldKey와 label이 있다")
        void shouldHaveFieldKeyAndLabel(CreatorCategory category) {
            ScriptTemplate template = provider.getTemplate(category);

            template.getSections().stream()
                    .flatMap(section -> section.getFields().stream())
                    .forEach(field -> {
                        assertThat(field.getFieldKey()).isNotBlank();
                        assertThat(field.getLabel()).isNotBlank();
                        assertThat(field.getFieldType()).isNotBlank();
                    });
        }

        @ParameterizedTest
        @EnumSource(CreatorCategory.class)
        @DisplayName("목표 섹션에 targetMonthlyIncome 필드가 있다")
        void shouldHaveTargetMonthlyIncome(CreatorCategory category) {
            ScriptTemplate template = provider.getTemplate(category);

            ScriptSection goalSection = template.getSections().get(3);
            assertThat(goalSection.getSectionTitle()).isEqualTo("목표");

            List<String> fieldKeys = goalSection.getFields().stream()
                    .map(ScriptField::getFieldKey)
                    .toList();
            assertThat(fieldKeys).contains("targetMonthlyIncome");
        }

        @ParameterizedTest
        @EnumSource(CreatorCategory.class)
        @DisplayName("비용 섹션에 hourlyWage 필드가 있다")
        void shouldHaveHourlyWage(CreatorCategory category) {
            ScriptTemplate template = provider.getTemplate(category);

            ScriptSection costSection = template.getSections().get(2);
            assertThat(costSection.getSectionTitle()).isEqualTo("비용");

            List<String> fieldKeys = costSection.getFields().stream()
                    .map(ScriptField::getFieldKey)
                    .toList();
            assertThat(fieldKeys).contains("hourlyWage");
        }

        @ParameterizedTest
        @EnumSource(CreatorCategory.class)
        @DisplayName("시간 투입 섹션에 totalMonthlyHours 자동계산 필드가 있다")
        void shouldHaveTotalMonthlyHoursAutoCalculate(CreatorCategory category) {
            ScriptTemplate template = provider.getTemplate(category);

            ScriptSection timeSection = template.getSections().get(1);
            assertThat(timeSection.getSectionTitle()).isEqualTo("시간 투입");

            ScriptField totalHoursField = timeSection.getFields().stream()
                    .filter(f -> "totalMonthlyHours".equals(f.getFieldKey()))
                    .findFirst()
                    .orElse(null);

            // EMOTICON 카테고리는 productionHoursPerSet로 자동계산
            if (category != CreatorCategory.EMOTICON) {
                assertThat(totalHoursField).isNotNull();
                assertThat(totalHoursField.isAutoCalculate()).isTrue();
                assertThat(totalHoursField.getFormula()).isNotBlank();
            }
        }
    }

    @Nested
    @DisplayName("WEB_NOVEL 템플릿")
    class WebNovel {

        @Test
        @DisplayName("수익 구조에 연재 플랫폼 선택 필드가 있다")
        void shouldHavePlatformSelect() {
            ScriptTemplate template = provider.getTemplate(CreatorCategory.WEB_NOVEL);
            ScriptSection revenueSection = template.getSections().get(0);

            ScriptField platformField = findField(revenueSection, "platform");
            assertThat(platformField).isNotNull();
            assertThat(platformField.getFieldType()).isEqualTo("select");
            assertThat(platformField.getOptions()).contains("KAKAO_PAGE", "MUNPIA", "NAVER_SERIES");
        }

        @Test
        @DisplayName("회당 판매가와 정산 비율 필드가 있다")
        void shouldHavePriceAndShareRate() {
            ScriptTemplate template = provider.getTemplate(CreatorCategory.WEB_NOVEL);
            ScriptSection revenueSection = template.getSections().get(0);

            assertThat(findField(revenueSection, "episodePrice")).isNotNull();
            assertThat(findField(revenueSection, "revenueShareRate")).isNotNull();
            assertThat(findField(revenueSection, "avgReadersPerEpisode")).isNotNull();
        }
    }

    @Nested
    @DisplayName("SHORT_FORM 템플릿")
    class ShortForm {

        @Test
        @DisplayName("수익 구조에 RPM과 조회수 필드가 있다")
        void shouldHaveViewsAndRpm() {
            ScriptTemplate template = provider.getTemplate(CreatorCategory.SHORT_FORM);
            ScriptSection revenueSection = template.getSections().get(0);

            assertThat(findField(revenueSection, "avgViewsPerVideo")).isNotNull();
            assertThat(findField(revenueSection, "rpmRate")).isNotNull();
            assertThat(findField(revenueSection, "videosPerMonth")).isNotNull();
        }
    }

    @Nested
    @DisplayName("EMOTICON 템플릿")
    class Emoticon {

        @Test
        @DisplayName("수익 구조에 세트 판매가와 정산 비율이 있다")
        void shouldHaveSetPriceAndShareRate() {
            ScriptTemplate template = provider.getTemplate(CreatorCategory.EMOTICON);
            ScriptSection revenueSection = template.getSections().get(0);

            assertThat(findField(revenueSection, "setPrice")).isNotNull();
            assertThat(findField(revenueSection, "revenueShareRate")).isNotNull();
            assertThat(findField(revenueSection, "monthlySales")).isNotNull();
        }
    }

    @Nested
    @DisplayName("INDIE_DEV 템플릿")
    class IndieDev {

        @Test
        @DisplayName("수익 구조에 제품 유형과 과금 모델이 있다")
        void shouldHaveProductAndPricingModel() {
            ScriptTemplate template = provider.getTemplate(CreatorCategory.INDIE_DEV);
            ScriptSection revenueSection = template.getSections().get(0);

            ScriptField productType = findField(revenueSection, "productType");
            assertThat(productType).isNotNull();
            assertThat(productType.getFieldType()).isEqualTo("select");
            assertThat(productType.getOptions()).contains("SAAS", "MOBILE_APP");

            assertThat(findField(revenueSection, "pricingModel")).isNotNull();
            assertThat(findField(revenueSection, "unitPrice")).isNotNull();
        }

        @Test
        @DisplayName("기회비용 시급 기본값이 개발자 시장 기준(15000)이다")
        void shouldHaveHigherDefaultHourlyWage() {
            ScriptTemplate template = provider.getTemplate(CreatorCategory.INDIE_DEV);
            ScriptSection costSection = template.getSections().get(2);

            ScriptField hourlyWage = findField(costSection, "hourlyWage");
            assertThat(hourlyWage).isNotNull();
            assertThat(hourlyWage.getDefaultValue().toString()).isEqualTo("15000");
        }
    }

    private ScriptField findField(ScriptSection section, String fieldKey) {
        return section.getFields().stream()
                .filter(f -> fieldKey.equals(f.getFieldKey()))
                .findFirst()
                .orElse(null);
    }
}
