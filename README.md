# Profit Logic Backend

본업과 사이드 프로젝트를 병행하는 1인 크리에이터를 위한 수익성 분석 API 서버입니다.

## 기술 스택

- Java 21 / Spring Boot 3.5
- Spring Security + JWT 인증
- Spring Data JPA + H2 (개발) / MySQL (운영)
- Spring WebFlux (LLM 클라이언트)
- Lombok / JUnit 5 / Mockito

## 핵심 기능

- **프로젝트 CRUD** — 크리에이터 유형별 사이드 프로젝트 관리
- **수익성 분석 엔진** — BEP(월 최소 건수), 실질 시급, 비용 구조, 안전마진 계산
- **AI 채팅 상담** — LLM 기반 맞춤 상담 + 규칙 기반 폴백
- **비용/시간 기록** — 상세 비용 항목, 일별 작업시간 트래킹
- **리포트 생성** — AI 기반 종합 분석 리포트
- **커뮤니티** — 게시글/댓글 CRUD
- **대시보드** — 프로젝트별 인사이트 요약, 월별 추이

## 실행 방법

```bash
# 빌드 및 테스트
./gradlew clean build

# 개발 서버 실행 (H2 인메모리 DB)
./gradlew bootRun

# 테스트만 실행
./gradlew test
```

서버는 기본 `http://localhost:8080`에서 실행됩니다.

## 프로젝트 구조

```
src/main/java/com/wookidoki/profitlogic/
├── config/          # Security, JWT, Jackson, CORS 설정
├── controller/      # REST 컨트롤러
├── service/         # 비즈니스 로직
├── domain/          # JPA 엔티티
├── repository/      # Spring Data JPA 리포지토리
├── dto/             # 요청/응답 DTO (15개 하위 패키지)
├── client/          # LLM 클라이언트, 프롬프트 템플릿
└── common/          # 예외 처리, 공통 유틸
```

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `JWT_SECRET` | JWT 서명 키 | 개발용 기본값 |
| `OPENAI_API_KEY` | LLM API 키 (없으면 규칙 기반 폴백) | — |
| `SPRING_DATASOURCE_URL` | DB 접속 URL | H2 인메모리 |

## 시드 데이터

개발 환경에서 `DataInitializer`가 자동으로 25+ 페르소나 계정과 샘플 데이터를 생성합니다.

- 데모 계정: `demo@profitlogic.com` / `demo1234`
- 관리자: `admin@profitlogic.com` / `admin1234`
