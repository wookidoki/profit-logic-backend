# Phase 4: 코드 품질 개선 기록

> Phase 4에서 수행한 코드 품질 개선 작업을 **Before/After + 수치**로 기록합니다.

---

## 요약 대시보드

| 지표 | Before | After | 개선율 |
| --- | --- | --- | --- |
| ConsultPage | 882줄 | 487줄 (+ 분리 2파일) | -45% |
| ScriptAnalysis | 803줄 | 429줄 (+ 분리 1파일) | -47% |
| 미사용 컴포넌트 | 4개 (344줄) | 0개 | 삭제 |
| 폼 중복 styled | 26개 (13×2 파일) | 공통 6개 | -77% |
| Error Boundary | 없음 | 전역 적용 | 신규 |
| 로딩 UI | 텍스트 10개 | 스피너 10개 | 교체 |
| BE N+1 쿼리 | 1곳 (AdminService) | 0곳 | 해결 |

---

## R1. ConsultPage 분리 (882줄 → 3파일)

### Before

하나의 파일에 카테고리 데이터(130줄), styled-components(325줄), 컴포넌트 로직(430줄)이 전부 포함.

### After

| 파일 | 역할 | 줄 수 |
| --- | --- | --- |
| `src/constants/categories.ts` | CATEGORIES 배열 + 인터페이스 | 106줄 |
| `src/pages/styles/ConsultPage.styles.ts` | styled-components | 325줄 |
| `src/pages/ConsultPage.tsx` | 컴포넌트 로직만 | 487줄 |

### 분리 기준

- **데이터**: 변경 빈도가 낮고 재사용 가능 → `constants/`
- **스타일**: 비즈니스 로직과 무관 → `styles/`
- **로직**: 상태 관리 + 이벤트 핸들러 + 렌더링 → 본체

---

## R2. ScriptAnalysis 스타일 분리 (803줄 → 2파일)

### Before

styled-components 418줄이 컴포넌트 파일에 혼재.

### After

| 파일 | 역할 | 줄 수 |
| --- | --- | --- |
| `src/pages/styles/ScriptAnalysis.styles.ts` | styled-components | 418줄 |
| `src/pages/ScriptAnalysis.tsx` | 컴포넌트 로직만 | 429줄 |

---

## R3. 미사용 컴포넌트 삭제 (-344줄)

`grep -rn "import.*ComponentName"` 으로 전체 프로젝트 검색 → import 0건 확인 후 삭제:

| 컴포넌트 | 사유 | 줄 수 |
| --- | --- | --- |
| `CostForm.tsx` | CostDetailPanel에서 인라인 폼 사용 | 87줄 |
| `TimeLogForm.tsx` | TimeLogPanel에서 인라인 폼 사용 | 92줄 |
| `AiParseModal.tsx` | 기능 제거됨 | 115줄 |
| `ProjectInputForm.tsx` | Create/Edit가 각각 자체 폼 | 50줄 |

---

## R4. ProjectCreate/Edit 폼 중복 제거

### Before

두 파일이 동일한 styled-components 13개를 각각 선언:

```
ProjectCreate.tsx: FormGroup, FormLabel, FormInput, FormHelper ... (13개)
ProjectEdit.tsx:   FormGroup, FormLabel, FormInput, FormHelper ... (13개, 동일)
```

### After

`src/styles/shared.ts`에 공통 폼 스타일 6개 추가:

```tsx
export const FormCard, FormGroup, FormLabel, FormInput, FormSelect, FormHelper
```

두 파일에서 로컬 선언 제거 → shared에서 import.

---

## R5. UI 라벨 크리에이터 리프레이밍 (9개 파일)

### 변경 전/후 비교

| 기존 용어 | 변경 후 | 이유 |
| --- | --- | --- |
| 판매가 | 건당 수익 | 크리에이터는 "판매"보다 "건당" 개념이 직관적 |
| 변동비 | 건당 비용 | 회계 용어 제거 |
| 고정비 | 월 고정 지출 | "비"보다 "지출"이 명확 |
| 공헌이익 | 건당 순수익 | 관리회계 전문용어 → 일반 용어 |
| 손익분기점 N개 | 월 최소 N건 | "개"보다 "건"이 크리에이터에 자연스러움 |
| 영업이익 | 월 수익 | 직관적 |
| 생존 가능성 | 지속 가능성 | 긍정적 프레이밍 |

### 적용 파일

ProjectCreate, ProjectEdit, ProjectDetail, ProjectList, Dashboard, Landing, ConsultPage, EnhancedAnalysisPanel, ResultCards

---

## R6. ChatService 완전 재작성

### Before (302줄)

- `generateRuleBasedAnswer`: 단순 추천 질문 목록 반환
- "안녕" 입력 → 추천 질문 6개만 표시
- 크리에이터 용어 미적용 ("판매가", "변동비" 사용)

### After (새 버전)

- 14개 분리 메서드: `isGreeting`, `answerBep`, `answerWage`, `answerGrowth`, `answerProfit`, `answerCost`, `answerPrice`, `answerTime`, `answerStrategy`, `answerSummary`, `answerGoal`, `answerCompare`, `answerDefault`
- "안녕" → 프로젝트 핵심 지표 요약 + 인사이트
- "성장성" → 건당 순수익률 + BEP 난이도 + 본업 시급 대비 평가
- 기본 응답 → 항상 프로젝트 데이터 포함 (빈 목록 절대 없음)
- 유틸리티: `matchAny`, `calcBep`, `calcRate`, `fmt`

---

## R7. 인프라 개선

### Error Boundary (신규)

- `ErrorBoundary.tsx` 클래스 컴포넌트 추가
- App.tsx에서 전역 래핑
- 에러 시 "문제가 발생했습니다" + 새로고침 버튼

### LoadingSpinner (교체)

- 기존: `<p>로딩 중...</p>` (10개 페이지)
- 변경: `<LoadingSpinner />` 애니메이션 스피너 + 텍스트

### index.html 메타태그

- `lang="en"` → `lang="ko"`
- `<title>profit-logic-frontend</title>` → `<title>Profit Logic — 크리에이터 수익 분석</title>`
- OG 메타태그 (og:title, og:description, og:type) 추가
- SEO description 추가
- favicon: vite.svg → 커스텀 favicon.svg (브랜드 컬러 P 로고)

### AdminService N+1 해결

- Before: 사용자 N명 → SQL N+1개
- After: GROUP BY JPQL → SQL 2개
