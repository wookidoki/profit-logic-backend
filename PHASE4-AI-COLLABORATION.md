# Phase 4: AI 협업 프로세스 기록

> Claude Code(Opus 4.6)와의 협업에서 **어떤 컨텍스트를 어떻게 제공했는지**, 그리고 **AI 결과물을 어떻게 검증/수정했는지**를 기록합니다.

---

## 1. 컨텍스트 관리 전략

### CLAUDE.md 자동 로드

매 세션 시작 시 AI가 프로젝트 규칙을 자동으로 읽도록 `CLAUDE.md` 파일을 프로젝트 루트에 배치했습니다.

포함 내용:
- 프로젝트 목적 (1인 크리에이터 수익성 분석)
- 기술 스택 (Spring Boot 3.5, React 19, TypeScript)
- 도메인 용어 규칙 (건당 수익, 월 최소 건수 등)
- 코드 컨벤션 (BigDecimal 필수, snake_case JSON)
- 금지 사항 (double/float 사용 금지, any 타입 금지)

### 프롬프트 파일 분리

하나의 거대한 프롬프트 대신, **작업 단위별로 프롬프트 파일을 분리**하여 관리했습니다.

| 파일 | 목적 | 라인 수 |
| --- | --- | --- |
| prompt-critical-fixes.md | 긴급 버그 4건 수정 | 385줄 |
| prompt-02-ui-reframing.md | UI 라벨 크리에이터 관점 전환 | 123줄 |
| prompt-03-full-refactoring.md | 대규모 코드 리팩토링 (4파트) | 758줄 |
| prompt-04-quality-ux.md | 코드 품질 + UX 개선 (8파트) | 331줄 |

---

## 2. 프롬프트별 상세 기록

### Prompt #1: 긴급 버그 4건 (prompt-critical-fixes.md)

**제공한 컨텍스트:**
- AdminApi의 현재 타입 정의 (camelCase)
- 백엔드 Jackson SNAKE_CASE 설정 내용
- ConsultPage 저장 시 누락되는 필드 목록
- 데모 로그인 계정의 현재 동작 상태

**AI에게 요청한 내용:**
1. AdminApi 타입을 snake_case로 수정 + AdminPage 필드 접근 수정
2. ConsultPage 저장 시 creator_category 전송 추가
3. 데모 로그인 시 프로젝트 로딩 실패 수정
4. AI 챗봇 추천 질문을 크리에이터 관점으로 변경

**AI 결과 검증:**
- AdminApi 타입 수정 → 정상 동작 확인
- creator_category 전송 → DB 저장 확인
- ChatService 규칙 기반 재작성 → **미적용 발견** → prompt-03에서 재요청

### Prompt #2: UI 라벨 리프레이밍 (prompt-02-ui-reframing.md)

**제공한 컨텍스트:**
- 용어 매핑 테이블 (판매가→건당 수익, 변동비→건당 비용 등 13쌍)
- 변경 금지 범위 (변수명, API 필드명, DB 컬럼명)
- 대상 파일 9개와 각 파일에서 변경할 구체적 위치

**프롬프트 설계 의도:**
"변수명은 그대로 두고 화면 텍스트만 변경"이라는 제약을 명시하여, AI가 코드 로직을 건드리는 것을 방지.

**AI 결과 검증:**
- 9개 파일 라벨 변경 완료
- `npx tsc --noEmit` 타입 에러 0
- 변수명 변경 없음 확인 (`git diff --stat`으로 변경 범위 확인)

### Prompt #3: 대규모 리팩토링 (prompt-03-full-refactoring.md)

**제공한 컨텍스트:**
- ChatService.java 현재 코드 전문 (302줄)
- 기존 코드의 문제점 (`grep` 결과로 증명: 인사/성장 패턴 0건)
- 크리에이터 용어 규칙
- 14개 패턴 매칭 설계 + 각 패턴의 응답 로직
- ConsultPage 현재 코드 (882줄) + 분리 목표
- ScriptAnalysis 현재 코드 (803줄) + 분리 목표

**4개 파트로 구조화:**

```
Part 1: ChatService 완전 교체 (최우선)
  → 14개 분리 메서드 + 크리에이터 용어 + 유틸리티

Part 2: PromptTemplates 크리에이터 관점 교체
  → 시스템 프롬프트 재설계

Part 3: 프론트엔드 리팩토링 (5개 하위 작업)
  → shared.ts 공통 컴포넌트, ConsultPage 분리, ScriptAnalysis 분리

Part 4: 백엔드 리팩토링
  → ScriptTemplateProvider 분리, BigDecimal 상수 정리
```

**AI 결과 검증:**
- ChatService 14패턴 동작 확인 (`grep "안녕\|성장" ChatService.java` → 매칭 성공)
- ConsultPage 882→487줄 분리 완료
- ScriptAnalysis 803→429줄 분리 완료
- `./gradlew clean build` 300+ 테스트 통과
- `npx tsc --noEmit` 에러 0

### Prompt #4: 코드 품질 + UX (prompt-04-quality-ux.md)

**제공한 컨텍스트:**
- `grep` 결과로 발견한 정량적 문제:
  - 하드코딩 색상 198개
  - ChatService java.math 풀경로 30개
  - 미사용 컴포넌트 4개 (import 검색 결과)
  - ProjectCreate/Edit 폼 스타일 중복 13개씩
  - Error Boundary 0개, 접근성 속성 8개

**8개 파트로 구조화 (A~H):**

```
A: 하드코딩 색상 → theme 적용 (198개)
B: ChatService java.math 풀경로 → import
C: AdminService N+1 쿼리 → GROUP BY
D: 미사용 컴포넌트 4개 삭제
E: ProjectCreate/Edit 폼 중복 제거
F: index.html 메타태그 + favicon
G: Error Boundary 추가
H: LoadingSpinner 컴포넌트
```

**AI 결과 검증:**
- theme 적용 후 `grep -c "#4361ee" src/pages/` → 0개
- N+1 → JPQL 전환 후 테스트 통과
- hex+alpha (`#4361ee10`) 4개 파일 수동 보정 필요 → 직접 수정

---

## 3. AI 결과물 검증 체크리스트

모든 AI 생성 코드에 대해 아래 검증을 수행했습니다:

| # | 검증 항목 | 방법 |
| --- | --- | --- |
| 1 | 타입 안전성 | `npx tsc --noEmit` |
| 2 | 백엔드 빌드 + 테스트 | `./gradlew clean build` (300+ 테스트) |
| 3 | 코드 적용 여부 | `grep`으로 키워드 존재 확인 |
| 4 | 변경 범위 | `git diff --stat`으로 의도하지 않은 변경 확인 |
| 5 | 엣지 케이스 | 0 나누기, null 입력, 빈 배열 등 수동 테스트 |
| 6 | 용어 일관성 | "판매가" 등 기존 용어가 남아있지 않은지 검색 |

---

## 4. AI 협업에서 발견한 패턴

### 효과적이었던 것

| 패턴 | 효과 |
| --- | --- |
| **프롬프트에 grep 결과 포함** | "현재 코드가 이렇다"는 증거를 보여주면 AI가 정확한 수정 제안 |
| **변경 금지 범위 명시** | "변수명, API 필드, DB 컬럼은 변경 금지"로 부수 효과 방지 |
| **작업 단위 분리** | 한 프롬프트에 1개 주제 → 검증이 쉬움 |
| **정량적 목표 제시** | "198개 → 0개", "882줄 → 430줄 목표" → AI가 완료 기준을 알 수 있음 |
| **코드 매핑 테이블** | 색상 매핑, 용어 매핑을 표로 정리 → 일관적 변환 |

### 주의가 필요했던 것

| 패턴 | 문제 | 대응 |
| --- | --- | --- |
| **"완료했습니다" 맹신** | AI가 완료를 보고했지만 실제 미적용 (Issue #5) | 반드시 `grep`/빌드로 확인 |
| **긴 대화 컨텍스트 유실** | 20턴 이상 대화하면 초기 규칙을 잊음 | CLAUDE.md + 세션 분리 |
| **일괄 치환의 함정** | hex+alpha 같은 변형 패턴 누락 | 치환 후 수동 검증 필수 |
| **과도한 추상화** | 간단한 유틸에 Strategy 패턴 적용 | "최소 복잡도" 제약 명시 |
