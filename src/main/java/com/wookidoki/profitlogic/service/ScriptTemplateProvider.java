package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.domain.CreatorCategory;
import com.wookidoki.profitlogic.dto.script.ScriptField;
import com.wookidoki.profitlogic.dto.script.ScriptSection;
import com.wookidoki.profitlogic.dto.script.ScriptTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ScriptTemplateProvider {

    private final Map<CreatorCategory, ScriptTemplate> templates;

    public ScriptTemplateProvider() {
        this.templates = new EnumMap<>(CreatorCategory.class);
        templates.put(CreatorCategory.WEB_NOVEL, buildWebNovelTemplate());
        templates.put(CreatorCategory.SHORT_FORM, buildShortFormTemplate());
        templates.put(CreatorCategory.EMOTICON, buildEmoticonTemplate());
        templates.put(CreatorCategory.BLOG, buildBlogTemplate());
        templates.put(CreatorCategory.INDIE_DEV, buildIndieDevTemplate());
    }

    public ScriptTemplate getTemplate(CreatorCategory category) {
        return templates.get(category);
    }

    // ── WEB_NOVEL ──────────────────────────────────────────

    private ScriptTemplate buildWebNovelTemplate() {
        return ScriptTemplate.builder()
                .category(CreatorCategory.WEB_NOVEL)
                .displayName(CreatorCategory.WEB_NOVEL.getDisplayName())
                .description(CreatorCategory.WEB_NOVEL.getDescription())
                .sections(List.of(
                        buildWebNovelRevenueSection(),
                        buildWebNovelTimeSection(),
                        buildWebNovelCostSection(),
                        buildWebNovelGoalSection()
                ))
                .build();
    }

    private ScriptSection buildWebNovelRevenueSection() {
        return ScriptSection.builder()
                .sectionTitle("수익 구조")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("platform")
                                .label("연재 플랫폼")
                                .fieldType("select")
                                .defaultValue("KAKAO_PAGE")
                                .options(List.of("KAKAO_PAGE", "MUNPIA", "NAVER_SERIES", "RIDI", "OTHER"))
                                .required(true)
                                .helpText("주요 수익이 발생하는 플랫폼을 선택하세요")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("episodePrice")
                                .label("회당 판매가")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("300"))
                                .placeholder("예: 300")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .max(new BigDecimal("10000"))
                                .helpText("독자가 1회차를 구매할 때 지불하는 금액")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("revenueShareRate")
                                .label("정산 비율")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("70"))
                                .placeholder("예: 70")
                                .unit("%")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .max(new BigDecimal("100"))
                                .helpText("플랫폼에서 작가에게 지급하는 비율")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("avgReadersPerEpisode")
                                .label("회당 평균 열람 수")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("500"))
                                .placeholder("예: 500")
                                .unit("명")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("한 회차를 유료로 읽는 평균 독자 수")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("episodesPerMonth")
                                .label("월 연재 회차 수")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("20"))
                                .placeholder("예: 20")
                                .unit("회")
                                .required(true)
                                .min(new BigDecimal("1"))
                                .max(new BigDecimal("60"))
                                .helpText("한 달에 올리는 회차 수")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildWebNovelTimeSection() {
        return ScriptSection.builder()
                .sectionTitle("시간 투입")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("writingHoursPerEpisode")
                                .label("회당 집필 시간")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("4"))
                                .placeholder("예: 4")
                                .unit("시간")
                                .required(true)
                                .min(new BigDecimal("0.5"))
                                .max(new BigDecimal("24"))
                                .helpText("한 회차를 쓰는 데 걸리는 평균 시간")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("editingHoursPerEpisode")
                                .label("회당 퇴고/교정 시간")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("1"))
                                .placeholder("예: 1")
                                .unit("시간")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .max(new BigDecimal("12"))
                                .helpText("퇴고·교정에 걸리는 시간")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("totalMonthlyHours")
                                .label("월 총 작업 시간")
                                .fieldType("number")
                                .unit("시간")
                                .required(true)
                                .autoCalculate(true)
                                .formula("(writingHoursPerEpisode + editingHoursPerEpisode) * episodesPerMonth")
                                .helpText("자동 계산: (집필 + 퇴고) × 월 회차 수")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildWebNovelCostSection() {
        return ScriptSection.builder()
                .sectionTitle("비용")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("platformFeeRate")
                                .label("플랫폼 수수료율")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("30"))
                                .unit("%")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .max(new BigDecimal("100"))
                                .autoCalculate(true)
                                .formula("100 - revenueShareRate")
                                .helpText("자동 계산: 100 - 정산 비율")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("monthlyFixedCost")
                                .label("월 고정비")
                                .fieldType("number")
                                .defaultValue(BigDecimal.ZERO)
                                .placeholder("예: 50000")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("자료 구독, 작업 공간 비용 등")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("hourlyWage")
                                .label("기회비용 시급")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("9860"))
                                .placeholder("예: 9860")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("다른 일을 했을 때 벌 수 있는 시급 (2024 최저시급 기준)")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildWebNovelGoalSection() {
        return ScriptSection.builder()
                .sectionTitle("목표")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("targetMonthlyIncome")
                                .label("월 목표 수입")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("2000000"))
                                .placeholder("예: 2000000")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("세전 월 목표 수입")
                                .build()
                ))
                .build();
    }

    // ── SHORT_FORM ──────────────────────────────────────────

    private ScriptTemplate buildShortFormTemplate() {
        return ScriptTemplate.builder()
                .category(CreatorCategory.SHORT_FORM)
                .displayName(CreatorCategory.SHORT_FORM.getDisplayName())
                .description(CreatorCategory.SHORT_FORM.getDescription())
                .sections(List.of(
                        buildShortFormRevenueSection(),
                        buildShortFormTimeSection(),
                        buildShortFormCostSection(),
                        buildShortFormGoalSection()
                ))
                .build();
    }

    private ScriptSection buildShortFormRevenueSection() {
        return ScriptSection.builder()
                .sectionTitle("수익 구조")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("mainPlatform")
                                .label("주요 플랫폼")
                                .fieldType("select")
                                .defaultValue("YOUTUBE_SHORTS")
                                .options(List.of("YOUTUBE_SHORTS", "INSTAGRAM_REELS", "TIKTOK", "MULTI"))
                                .required(true)
                                .helpText("주 수익 플랫폼을 선택하세요")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("revenueModel")
                                .label("수익 모델")
                                .fieldType("select")
                                .defaultValue("AD_REVENUE")
                                .options(List.of("AD_REVENUE", "SPONSORSHIP", "AFFILIATE", "MIXED"))
                                .required(true)
                                .helpText("주요 수익원을 선택하세요")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("avgViewsPerVideo")
                                .label("영상당 평균 조회수")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("10000"))
                                .placeholder("예: 10000")
                                .unit("회")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("최근 30일 기준 영상당 평균 조회수")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("rpmRate")
                                .label("RPM (1,000회당 수익)")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("800"))
                                .placeholder("예: 800")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("조회수 1,000회당 광고 수익")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("videosPerMonth")
                                .label("월 업로드 수")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("20"))
                                .placeholder("예: 20")
                                .unit("개")
                                .required(true)
                                .min(new BigDecimal("1"))
                                .max(new BigDecimal("100"))
                                .helpText("한 달에 올리는 영상 수")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("sponsorshipPerVideo")
                                .label("영상당 협찬/광고비")
                                .fieldType("number")
                                .defaultValue(BigDecimal.ZERO)
                                .placeholder("예: 50000")
                                .unit("원")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .helpText("협찬/PPL 등 영상당 추가 수익")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildShortFormTimeSection() {
        return ScriptSection.builder()
                .sectionTitle("시간 투입")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("filmingHoursPerVideo")
                                .label("영상당 촬영 시간")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("1"))
                                .placeholder("예: 1")
                                .unit("시간")
                                .required(true)
                                .min(new BigDecimal("0.1"))
                                .max(new BigDecimal("24"))
                                .helpText("촬영에 걸리는 시간")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("editingHoursPerVideo")
                                .label("영상당 편집 시간")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("2"))
                                .placeholder("예: 2")
                                .unit("시간")
                                .required(true)
                                .min(new BigDecimal("0.1"))
                                .max(new BigDecimal("24"))
                                .helpText("편집에 걸리는 시간")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("totalMonthlyHours")
                                .label("월 총 작업 시간")
                                .fieldType("number")
                                .unit("시간")
                                .required(true)
                                .autoCalculate(true)
                                .formula("(filmingHoursPerVideo + editingHoursPerVideo) * videosPerMonth")
                                .helpText("자동 계산: (촬영 + 편집) × 월 업로드 수")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildShortFormCostSection() {
        return ScriptSection.builder()
                .sectionTitle("비용")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("equipmentMonthlyCost")
                                .label("장비 월 감가비")
                                .fieldType("number")
                                .defaultValue(BigDecimal.ZERO)
                                .placeholder("예: 100000")
                                .unit("원")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .helpText("카메라, 조명 등 장비 감가상각 월 비용")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("softwareMonthlyCost")
                                .label("편집 SW 월 비용")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("20000"))
                                .placeholder("예: 20000")
                                .unit("원")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .helpText("편집 프로그램 구독료 (프리미어 프로 등)")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("monthlyFixedCost")
                                .label("월 고정비 합계")
                                .fieldType("number")
                                .unit("원")
                                .required(true)
                                .autoCalculate(true)
                                .formula("equipmentMonthlyCost + softwareMonthlyCost")
                                .helpText("자동 계산: 장비 감가비 + SW 비용")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("hourlyWage")
                                .label("기회비용 시급")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("9860"))
                                .placeholder("예: 9860")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("다른 일을 했을 때 벌 수 있는 시급")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildShortFormGoalSection() {
        return ScriptSection.builder()
                .sectionTitle("목표")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("targetMonthlyIncome")
                                .label("월 목표 수입")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("1500000"))
                                .placeholder("예: 1500000")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("세전 월 목표 수입")
                                .build()
                ))
                .build();
    }

    // ── EMOTICON ──────────────────────────────────────────

    private ScriptTemplate buildEmoticonTemplate() {
        return ScriptTemplate.builder()
                .category(CreatorCategory.EMOTICON)
                .displayName(CreatorCategory.EMOTICON.getDisplayName())
                .description(CreatorCategory.EMOTICON.getDescription())
                .sections(List.of(
                        buildEmoticonRevenueSection(),
                        buildEmoticonTimeSection(),
                        buildEmoticonCostSection(),
                        buildEmoticonGoalSection()
                ))
                .build();
    }

    private ScriptSection buildEmoticonRevenueSection() {
        return ScriptSection.builder()
                .sectionTitle("수익 구조")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("platform")
                                .label("판매 플랫폼")
                                .fieldType("select")
                                .defaultValue("KAKAO")
                                .options(List.of("KAKAO", "LINE", "APPLE", "MULTI"))
                                .required(true)
                                .helpText("이모티콘을 판매하는 플랫폼")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("setPrice")
                                .label("세트 판매가")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("2500"))
                                .placeholder("예: 2500")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("이모티콘 1세트의 판매 가격")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("revenueShareRate")
                                .label("정산 비율")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("33"))
                                .placeholder("예: 33")
                                .unit("%")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .max(new BigDecimal("100"))
                                .helpText("플랫폼에서 크리에이터에게 지급하는 비율 (카카오 약 33%)")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("monthlySales")
                                .label("월 예상 판매량")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("200"))
                                .placeholder("예: 200")
                                .unit("세트")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("한 달 기준 예상 판매 세트 수")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("numberOfSets")
                                .label("판매 중인 세트 수")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("1"))
                                .placeholder("예: 1")
                                .unit("세트")
                                .required(true)
                                .min(new BigDecimal("1"))
                                .helpText("현재 판매 중인 이모티콘 세트 수")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildEmoticonTimeSection() {
        return ScriptSection.builder()
                .sectionTitle("시간 투입")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("hoursPerEmoticon")
                                .label("이모티콘 1개당 제작 시간")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("3"))
                                .placeholder("예: 3")
                                .unit("시간")
                                .required(true)
                                .min(new BigDecimal("0.5"))
                                .max(new BigDecimal("48"))
                                .helpText("이모티콘 1개 그리는 데 걸리는 시간")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("emoticonsPerSet")
                                .label("세트당 이모티콘 수")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("24"))
                                .placeholder("예: 24")
                                .unit("개")
                                .required(true)
                                .min(new BigDecimal("1"))
                                .helpText("한 세트에 포함되는 이모티콘 개수")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("productionHoursPerSet")
                                .label("세트당 총 제작 시간")
                                .fieldType("number")
                                .unit("시간")
                                .required(true)
                                .autoCalculate(true)
                                .formula("hoursPerEmoticon * emoticonsPerSet")
                                .helpText("자동 계산: 개당 시간 × 세트당 개수")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("monthlyMaintenanceHours")
                                .label("월 유지/홍보 시간")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("10"))
                                .placeholder("예: 10")
                                .unit("시간")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .helpText("SNS 홍보, 고객 응대 등 월 운영 시간")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildEmoticonCostSection() {
        return ScriptSection.builder()
                .sectionTitle("비용")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("tabletMonthlyCost")
                                .label("태블릿/펜 월 감가비")
                                .fieldType("number")
                                .defaultValue(BigDecimal.ZERO)
                                .placeholder("예: 30000")
                                .unit("원")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .helpText("아이패드, 와콤 등 장비 감가상각 월 비용")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("softwareMonthlyCost")
                                .label("그래픽 SW 월 비용")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("11000"))
                                .placeholder("예: 11000")
                                .unit("원")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .helpText("프로크리에이트, 클립스튜디오 등 월 비용")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("monthlyFixedCost")
                                .label("월 고정비 합계")
                                .fieldType("number")
                                .unit("원")
                                .required(true)
                                .autoCalculate(true)
                                .formula("tabletMonthlyCost + softwareMonthlyCost")
                                .helpText("자동 계산: 태블릿 감가비 + SW 비용")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("hourlyWage")
                                .label("기회비용 시급")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("9860"))
                                .placeholder("예: 9860")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("다른 일을 했을 때 벌 수 있는 시급")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildEmoticonGoalSection() {
        return ScriptSection.builder()
                .sectionTitle("목표")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("targetMonthlyIncome")
                                .label("월 목표 수입")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("1000000"))
                                .placeholder("예: 1000000")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("세전 월 목표 수입")
                                .build()
                ))
                .build();
    }

    // ── BLOG ──────────────────────────────────────────

    private ScriptTemplate buildBlogTemplate() {
        return ScriptTemplate.builder()
                .category(CreatorCategory.BLOG)
                .displayName(CreatorCategory.BLOG.getDisplayName())
                .description(CreatorCategory.BLOG.getDescription())
                .sections(List.of(
                        buildBlogRevenueSection(),
                        buildBlogTimeSection(),
                        buildBlogCostSection(),
                        buildBlogGoalSection()
                ))
                .build();
    }

    private ScriptSection buildBlogRevenueSection() {
        return ScriptSection.builder()
                .sectionTitle("수익 구조")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("blogPlatform")
                                .label("블로그 플랫폼")
                                .fieldType("select")
                                .defaultValue("NAVER")
                                .options(List.of("NAVER", "TISTORY", "NEWSLETTER", "WORDPRESS", "OTHER"))
                                .required(true)
                                .helpText("주로 운영하는 플랫폼")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("revenueModel")
                                .label("수익 모델")
                                .fieldType("select")
                                .defaultValue("AD_REVENUE")
                                .options(List.of("AD_REVENUE", "AFFILIATE", "SPONSORED_POST", "PREMIUM_CONTENT", "MIXED"))
                                .required(true)
                                .helpText("주요 수익원을 선택하세요")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("monthlyPageViews")
                                .label("월 페이지뷰")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("50000"))
                                .placeholder("예: 50000")
                                .unit("PV")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("월간 총 페이지뷰 수")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("rpmRate")
                                .label("RPM (1,000PV당 수익)")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("1500"))
                                .placeholder("예: 1500")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("페이지뷰 1,000회당 광고 수익")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("postsPerMonth")
                                .label("월 게시물 수")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("15"))
                                .placeholder("예: 15")
                                .unit("개")
                                .required(true)
                                .min(new BigDecimal("1"))
                                .max(new BigDecimal("100"))
                                .helpText("한 달에 작성하는 게시물 수")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("affiliateIncomePerMonth")
                                .label("월 제휴/협찬 수익")
                                .fieldType("number")
                                .defaultValue(BigDecimal.ZERO)
                                .placeholder("예: 200000")
                                .unit("원")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .helpText("제휴 마케팅, 협찬 등 추가 수익")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildBlogTimeSection() {
        return ScriptSection.builder()
                .sectionTitle("시간 투입")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("writingHoursPerPost")
                                .label("게시물당 작성 시간")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("3"))
                                .placeholder("예: 3")
                                .unit("시간")
                                .required(true)
                                .min(new BigDecimal("0.5"))
                                .max(new BigDecimal("24"))
                                .helpText("리서치 + 작성 + 이미지 준비 시간")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("seoManagementHours")
                                .label("월 SEO/관리 시간")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("5"))
                                .placeholder("예: 5")
                                .unit("시간")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .helpText("키워드 분석, 댓글 관리, 통계 확인 등")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("totalMonthlyHours")
                                .label("월 총 작업 시간")
                                .fieldType("number")
                                .unit("시간")
                                .required(true)
                                .autoCalculate(true)
                                .formula("(writingHoursPerPost * postsPerMonth) + seoManagementHours")
                                .helpText("자동 계산: (게시물당 작성 × 월 게시물 수) + SEO 관리 시간")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildBlogCostSection() {
        return ScriptSection.builder()
                .sectionTitle("비용")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("hostingMonthlyCost")
                                .label("호스팅/도메인 월 비용")
                                .fieldType("number")
                                .defaultValue(BigDecimal.ZERO)
                                .placeholder("예: 10000")
                                .unit("원")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .helpText("웹호스팅, 도메인, CDN 등 비용 (네이버 블로그는 무료)")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("toolsMonthlyCost")
                                .label("도구/구독 월 비용")
                                .fieldType("number")
                                .defaultValue(BigDecimal.ZERO)
                                .placeholder("예: 20000")
                                .unit("원")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .helpText("이미지 편집, SEO 도구, 뉴스레터 서비스 등")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("monthlyFixedCost")
                                .label("월 고정비 합계")
                                .fieldType("number")
                                .unit("원")
                                .required(true)
                                .autoCalculate(true)
                                .formula("hostingMonthlyCost + toolsMonthlyCost")
                                .helpText("자동 계산: 호스팅 + 도구 비용")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("hourlyWage")
                                .label("기회비용 시급")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("9860"))
                                .placeholder("예: 9860")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("다른 일을 했을 때 벌 수 있는 시급")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildBlogGoalSection() {
        return ScriptSection.builder()
                .sectionTitle("목표")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("targetMonthlyIncome")
                                .label("월 목표 수입")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("1000000"))
                                .placeholder("예: 1000000")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("세전 월 목표 수입")
                                .build()
                ))
                .build();
    }

    // ── INDIE_DEV ──────────────────────────────────────────

    private ScriptTemplate buildIndieDevTemplate() {
        return ScriptTemplate.builder()
                .category(CreatorCategory.INDIE_DEV)
                .displayName(CreatorCategory.INDIE_DEV.getDisplayName())
                .description(CreatorCategory.INDIE_DEV.getDescription())
                .sections(List.of(
                        buildIndieDevRevenueSection(),
                        buildIndieDevTimeSection(),
                        buildIndieDevCostSection(),
                        buildIndieDevGoalSection()
                ))
                .build();
    }

    private ScriptSection buildIndieDevRevenueSection() {
        return ScriptSection.builder()
                .sectionTitle("수익 구조")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("productType")
                                .label("제품 유형")
                                .fieldType("select")
                                .defaultValue("SAAS")
                                .options(List.of("SAAS", "MOBILE_APP", "TEMPLATE", "PLUGIN", "COURSE", "OTHER"))
                                .required(true)
                                .helpText("개발/판매하는 제품 유형")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("pricingModel")
                                .label("과금 모델")
                                .fieldType("select")
                                .defaultValue("SUBSCRIPTION")
                                .options(List.of("SUBSCRIPTION", "ONE_TIME", "FREEMIUM", "PAY_PER_USE"))
                                .required(true)
                                .helpText("수익이 발생하는 방식")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("unitPrice")
                                .label("단위 가격")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("9900"))
                                .placeholder("예: 9900")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("구독료/1회 구매가/건당 가격")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("monthlyCustomers")
                                .label("월 고객 수 / 판매 수")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("50"))
                                .placeholder("예: 50")
                                .unit("명/건")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("월 구독자 수 또는 월 판매 건수")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("platformFeeRate")
                                .label("플랫폼/결제 수수료율")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("10"))
                                .placeholder("예: 10")
                                .unit("%")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .max(new BigDecimal("100"))
                                .helpText("앱스토어, Gumroad, Stripe 등 수수료")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildIndieDevTimeSection() {
        return ScriptSection.builder()
                .sectionTitle("시간 투입")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("developmentHoursPerMonth")
                                .label("월 개발 시간")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("80"))
                                .placeholder("예: 80")
                                .unit("시간")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("기능 개발, 버그 수정 등 코딩 시간")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("supportHoursPerMonth")
                                .label("월 고객 지원 시간")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("10"))
                                .placeholder("예: 10")
                                .unit("시간")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .helpText("고객 문의 응대, 문서 작성 등")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("marketingHoursPerMonth")
                                .label("월 마케팅 시간")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("10"))
                                .placeholder("예: 10")
                                .unit("시간")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .helpText("블로그 포스팅, SNS, 커뮤니티 홍보 등")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("totalMonthlyHours")
                                .label("월 총 작업 시간")
                                .fieldType("number")
                                .unit("시간")
                                .required(true)
                                .autoCalculate(true)
                                .formula("developmentHoursPerMonth + supportHoursPerMonth + marketingHoursPerMonth")
                                .helpText("자동 계산: 개발 + 고객 지원 + 마케팅")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildIndieDevCostSection() {
        return ScriptSection.builder()
                .sectionTitle("비용")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("serverMonthlyCost")
                                .label("서버/인프라 월 비용")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("50000"))
                                .placeholder("예: 50000")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("AWS, Vercel, DB, 도메인 등 인프라 비용")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("toolsMonthlyCost")
                                .label("개발 도구 월 비용")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("30000"))
                                .placeholder("예: 30000")
                                .unit("원")
                                .required(false)
                                .min(BigDecimal.ZERO)
                                .helpText("IDE, GitHub, CI/CD, 모니터링 도구 등")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("monthlyFixedCost")
                                .label("월 고정비 합계")
                                .fieldType("number")
                                .unit("원")
                                .required(true)
                                .autoCalculate(true)
                                .formula("serverMonthlyCost + toolsMonthlyCost")
                                .helpText("자동 계산: 서버 + 도구 비용")
                                .build(),
                        ScriptField.builder()
                                .fieldKey("hourlyWage")
                                .label("기회비용 시급")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("15000"))
                                .placeholder("예: 15000")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("개발자 시장 시급 기준")
                                .build()
                ))
                .build();
    }

    private ScriptSection buildIndieDevGoalSection() {
        return ScriptSection.builder()
                .sectionTitle("목표")
                .fields(List.of(
                        ScriptField.builder()
                                .fieldKey("targetMonthlyIncome")
                                .label("월 목표 수입")
                                .fieldType("number")
                                .defaultValue(new BigDecimal("3000000"))
                                .placeholder("예: 3000000")
                                .unit("원")
                                .required(true)
                                .min(BigDecimal.ZERO)
                                .helpText("세전 월 목표 수입")
                                .build()
                ))
                .build();
    }
}
