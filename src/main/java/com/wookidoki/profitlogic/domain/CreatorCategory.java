package com.wookidoki.profitlogic.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CreatorCategory {

    WEB_NOVEL("웹소설 작가", "카카오페이지, 문피아 등에서 연재하는 작가를 위한 분석"),
    SHORT_FORM("숏폼 크리에이터", "유튜브 쇼츠, 인스타 릴스, 틱톡 크리에이터를 위한 분석"),
    EMOTICON("이모티콘 셀러", "카카오 이모티콘, 라인 스티커 등 이모티콘 제작/판매를 위한 분석"),
    BLOG("블로그/뉴스레터", "네이버 블로그, 티스토리, 뉴스레터 운영자를 위한 분석"),
    INDIE_DEV("인디 개발자/SaaS", "SaaS, 앱, 플러그인, 템플릿 등 1인 개발자를 위한 분석");

    private final String displayName;
    private final String description;
}
