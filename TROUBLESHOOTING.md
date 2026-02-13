# Profit Logic - Troubleshooting & Security Audit

> 프로젝트 개발 과정에서 발견된 주요 이슈와 해결 과정을 기록한 문서.
> 동일한 실수를 반복하지 않기 위한 참고 자료.

---

## Table of Contents

### Bug & Architecture Issues
- [#1 프론트엔드 API 응답 필드 전부 null (snake_case 불일치)](#issue-1-프론트엔드-api-응답-필드-전부-null)
- [#2 AdminService N+1 쿼리](#issue-2-adminservice-사용자-목록-n1-쿼리)
- [#3 BEP 계산 ArithmeticException (0 나누기)](#issue-3-bep-계산-시-arithmeticexception)
- [#4 401 무한 리다이렉트](#issue-4-401-무한-리다이렉트)
- [#5 ChatService 규칙 기반 응답 미적용](#issue-5-chatservice-규칙-기반-응답-미적용)
- [#6 Docker Compose MySQL 커넥션 실패](#issue-6-docker-compose-mysql-커넥션-실패)
- [#7 하드코딩 색상 hex+alpha 누락](#issue-7-하드코딩-색상-theme-전환-시-hexalpha-누락)
- [#8 ConsultPage creator_category 미전송](#issue-8-consultpage에서-creator_category-미전송)
- [#9 MySQL year_month 예약어 INSERT 실패](#issue-9-mysql-year_month-예약어-insert-실패)

### Security Issues
- [#S1 JWT Secret 하드코딩](#security-1-jwt-secret-하드코딩)
- [#S2 DB 크리덴셜 소스코드 노출](#security-2-db-크리덴셜-소스코드-노출)
- [#S3 Gemini API Key URL 쿼리 파라미터 노출](#security-3-gemini-api-key-url-쿼리-파라미터-노출)
- [#S4 deploy.sh 평문 비밀키 포함](#security-4-deploysh-평문-비밀키-포함)
- [#S5 CORS 와일드카드 + Credentials](#security-5-cors-와일드카드--credentials)
- [#S6 CSRF 비활성화](#security-6-csrf-비활성화)
- [#S7 H2 Console 인증 없이 노출](#security-7-h2-console-인증-없이-노출)
- [#S8 DataInitializer 평문 패스워드 로깅](#security-8-datainitializer-평문-패스워드-로깅)
- [#S9 Validation 에러 상세 노출](#security-9-validation-에러-상세-노출)

---

## Bug & Architecture Issues

---

### Issue #1: 프론트엔드 API 응답 필드 전부 null

**심각도:** High | **상태:** Resolved

**증상:**
AdminPage에서 사용자 목록을 불러오면 닉네임, 프로젝트 수 등이 모두 `undefined`로 표시.

**원인:**
```yaml
# application.yml
spring.jackson.property-naming-strategy: SNAKE_CASE
```

백엔드 JSON 응답은 `snake_case`인데, 프론트 TypeScript 타입은 `camelCase`로 정의되어 있었다.

```
백엔드 응답: { "project_count": 3, "biz_type": "CREATOR" }
프론트 접근: data.projectCount → undefined
```

**해결:**
프론트 타입 정의를 `snake_case`로 통일.

```tsx
// Before
interface AdminUser { projectCount: number; bizType: string; }

// After
interface AdminUser { project_count: number; biz_type: string | null; }
```

**영향 범위:** AdminApi, AdminPage, DashboardApi 등 API 응답을 사용하는 전체 타입.

**교훈:**
> 백엔드 직렬화 전략(`SNAKE_CASE` / `camelCase`)은 프로젝트 초기에 확정하고,
> 프론트 타입과 반드시 일치시켜야 한다.
> 중간에 `SNAKE_CASE`를 추가하면 모든 타입을 수정해야 하는 비용이 발생한다.

---

### Issue #2: AdminService 사용자 목록 N+1 쿼리

**심각도:** Medium | **상태:** Resolved

**증상:**
관리자 페이지 사용자 목록 조회 시 사용자가 100명이면 SQL 쿼리 101개 실행.

**원인:**
```java
// 사용자마다 개별 쿼리 발생
users.stream().map(user -> AdminUserResponse.builder()
    .projectCount(projectRepository.findByUserId(user.getId()).size())  // N+1
    .build())
```

사용자 N명 → `SELECT 1`(전체) + `SELECT N`(프로젝트 수) = **N+1 쿼리**.

**해결:**
JPQL `GROUP BY`로 단일 쿼리 최적화.

```java
// ProjectRepository
@Query("SELECT p.user.id, COUNT(p) FROM Project p GROUP BY p.user.id")
List<Object[]> countProjectsGroupByUserId();

// AdminService — Map으로 변환 후 O(1) 조회
Map<Long, Long> projectCounts = projectRepository.countProjectsGroupByUserId()
    .stream().collect(Collectors.toMap(row -> (Long)row[0], row -> (Long)row[1]));
```

**검증:** 쿼리 로그로 SELECT 2개만 실행되는 것 확인 (users + count).

**교훈:**
> JPA에서 연관 엔티티의 COUNT를 루프 안에서 조회하면 N+1이 발생한다.
> 집계가 필요하면 항상 `GROUP BY` 쿼리를 별도로 작성할 것.

---

### Issue #3: BEP 계산 시 ArithmeticException

**심각도:** High | **상태:** Resolved

**증상:**
건당 수익 = 건당 비용인 프로젝트에서 BEP 계산 시 서버 500 에러.

**원인:**
```java
// BEP = 고정비 / (건당 수익 - 건당 비용)
// 건당 수익 == 건당 비용이면 분모 = 0
BigDecimal contribution = price.subtract(variableCost);  // = 0
return fixedCost.divide(contribution, 0, CEILING);        // ArithmeticException!
```

**해결:**
`FinancialCalculator`에 방어 로직 추가.

```java
private void validateContribution(BigDecimal contribution) {
    if (contribution.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessLogicException("팔수록 손해입니다");
    }
}
```

- CM = 0: "어떤 판매량에서도 고정비를 회수할 수 없습니다"
- CM < 0: "판매할수록 손해입니다. 가격을 올리거나 비용을 줄이세요"

**검증:**
```java
@Test
void BEP_공헌이익_0이면_예외() {
    assertThrows(BusinessLogicException.class,
        () -> calculator.calculateBEP(fixedCost, price, price));  // CM = 0
}
```

**교훈:**
> `BigDecimal.divide()`에서 분모가 0이면 `ArithmeticException`이 발생한다.
> 금융 계산에서는 항상 분모 검증을 먼저 수행할 것.

---

### Issue #4: 401 무한 리다이렉트

**심각도:** Critical | **상태:** Resolved

**증상:**
JWT 토큰 만료 후 페이지 접근 시 브라우저가 무한 새로고침.

**원인:**
```tsx
// 기존 axios 인터셉터
if (error.response?.status === 401) {
    window.location.href = '/login';  // 페이지 전체 리로드
    // 리로드 → 컴포넌트 마운트 → API 호출 → 401 → 리다이렉트 → 무한 반복
}
```

`window.location.href`는 SPA를 완전히 새로고침하므로, 컴포넌트가 다시 마운트되며 API를 재호출한다.
토큰이 여전히 만료 상태 → 401 → 리다이렉트 → 무한 루프.

**해결:**
```tsx
if (error.response?.status === 401) {
    useAuthStore.getState().logout();  // 토큰 제거 + 상태 초기화
    // ProtectedRoute가 상태 변화를 감지 → 로그인 페이지로 SPA 라우팅
}
```

**교훈:**
> SPA에서 인증 만료 처리는 `window.location` 대신 상태 관리 라이브러리(Zustand 등)를 통해야 한다.
> 전체 페이지 리로드는 SPA의 상태를 초기화하면서 동일한 API를 다시 호출하는 부작용을 낳는다.

---

### Issue #5: ChatService 규칙 기반 응답 미적용

**심각도:** Medium | **상태:** Resolved

**증상:**
AI 챗봇에서 "안녕"을 입력하면 추천 질문 목록만 반환. "성장성" 관련 질문에도 기본 응답만 표시.

**원인:**
이전 세션에서 ChatService 재작성을 요청했지만, **실제 파일에 코드가 적용되지 않았다.**
```bash
grep -n "안녕\|성장\|greet" ChatService.java  # → 매칭 0건
```
기존의 단순한 `generateRuleBasedAnswer` (추천 질문 목록 반환)이 그대로 남아있었다.

**해결:**
ChatService.java 전체 교체 (302줄 → 새 버전).
- `isGreeting()`: 인사 패턴 매칭 → 프로젝트 핵심 지표 요약 응답
- `matchAny()`: 키워드 배열 매칭 유틸
- `answerGrowth()`: 건당 순수익률 + BEP 난이도 + 본업 시급 대비 평가
- `answerDefault()`: 기본 응답 개선 (항상 프로젝트 데이터 포함)

**검증:** ChatServiceTest에서 14개 패턴 각각의 키워드 매칭 + 응답 내용 검증.

**교훈:**
> AI에게 코드 작성을 요청한 후 반드시 `grep`이나 실제 실행으로 적용 여부를 확인해야 한다.
> "작성 완료"라는 AI 응답만 믿으면 안 된다.

---

### Issue #6: Docker Compose MySQL 커넥션 실패

**심각도:** High | **상태:** Resolved

**증상:**
`docker-compose up` 실행 시 Spring Boot가 MySQL 연결에 실패하며 즉시 종료.

**원인:**
```yaml
# depends_on은 컨테이너 "시작"만 보장, "준비 완료"는 보장 안 함
depends_on:
  - mysql
```

MySQL 초기화(테이블 생성 등)에 10~15초가 소요되는데, Spring Boot가 먼저 연결을 시도해서 실패.

**해결:**
```yaml
services:
  mysql:
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    depends_on:
      mysql:
        condition: service_healthy  # healthy일 때만 시작
```

**교훈:**
> Docker Compose의 `depends_on`은 순서 보장일 뿐, 서비스 준비 보장이 아니다.
> DB 연결이 필요한 서비스는 반드시 `healthcheck` 조건을 걸어야 한다.

---

### Issue #7: 하드코딩 색상 theme 전환 시 hex+alpha 누락

**심각도:** Low | **상태:** Resolved

**증상:**
색상 토큰 전환 후 일부 투명도 배경이 검은색으로 표시.

**원인:**
CSS에서 `#4361ee10` (hex 6자리 + alpha 2자리)을 사용하는 곳이 있었다.
정규식으로 `#4361ee` → `${theme.colors.primary}`로 일괄 변환했으나,
`#4361ee10`이 `${theme.colors.primary}10`으로 변환되면서 CSS가 해석 불가.

```tsx
// 깨진 상태
background: ${theme.colors.primary}10;  // CSS가 이해 못함
```

**해결:**
```tsx
// 방법 1: 템플릿 리터럴 중첩
background: ${`${theme.colors.primary}10`};

// 방법 2: rgba로 변환
background: rgba(67, 97, 238, 0.06);
```

**교훈:**
> 일괄 치환(find & replace)은 패턴이 다른 변형을 놓칠 수 있다.
> 특히 `hex+alpha` 같은 CSS 특수 표기는 정규식 치환 후 수동 검증이 필요하다.

---

### Issue #8: ConsultPage에서 creator_category 미전송

**심각도:** Medium | **상태:** Resolved

**증상:**
맞춤 상담 → 프로젝트 저장 시 크리에이터 카테고리가 `null`로 저장됨.

**원인:**
프론트에서 스크립트 분석 → 프로젝트 저장 시 `creator_category` 필드를 포함하지 않았다.
백엔드 `ScriptSaveRequest`에 `creatorCategory` 필드는 있지만 프론트에서 미전송.

**해결:**
```tsx
const saveRequest = {
    ...analysisResult,
    creator_category: selectedCategory,  // 추가
};
```

**교훈:**
> API 요청/응답의 필드를 설계할 때,
> 프론트와 백엔드 양쪽의 DTO를 반드시 대조 확인해야 한다.

---

### Issue #9: MySQL year_month 예약어 INSERT 실패

**심각도:** Critical | **상태:** Resolved | **커밋:** `2a494a3`

**증상:**
`POST /v1/projects/{id}/reports?yearMonth=2026-02` 호출 시 500 에러.
AI(Gemini) 리포트 생성은 성공하지만 DB 저장 시 실패.

**에러 메시지:**
```
could not execute statement [You have an error in your SQL syntax...
near 'year_month) values ('## 2026년 2월...'
```

**원인:**
`YEAR_MONTH`은 **MySQL 8.0 예약어** (interval unit keyword).
Hibernate가 INSERT SQL을 생성할 때 컬럼명을 따옴표 없이 출력:

```sql
-- Hibernate 생성 SQL (문제)
INSERT INTO reports (content, project_id, tokens_used, year_month) VALUES (?, ?, ?, ?)
--                                                      ^^^^^^^^^^
-- MySQL이 year_month를 키워드로 파싱 → 문법 에러
```

SELECT는 `r.year_month`처럼 테이블 별칭이 붙어서 정상 동작했지만,
INSERT의 컬럼 리스트에서는 별칭 없이 `year_month`가 단독으로 나와 예약어 충돌.

**시도한 해결 방법들:**

| # | 방법 | 결과 |
|---|------|------|
| 1 | `@Column(name = "\`year_month\`")` (백틱 직접 지정) | H2 테스트 5개 실패 (대소문자 호환성) |
| 2 | `globally_quoted_identifiers: true` 전체 적용 | H2 테스트 62개 실패 (따옴표가 대소문자 구분 강제) |
| 3 | **MySQL에만 `globally_quoted_identifiers: true`, H2에는 `false`** | **295/295 전체 통과** |

**최종 해결:**
```yaml
# application.yml (MySQL 프로덕션)
spring.jpa.properties.hibernate.globally_quoted_identifiers: true

# application-local.yml (H2 로컬 개발)
spring.jpa.properties.hibernate.globally_quoted_identifiers: false

# application-test.yml (H2 테스트)
spring.jpa.properties.hibernate.globally_quoted_identifiers: false
```

이렇게 하면 MySQL에서는 모든 식별자가 백틱으로 감싸져서 예약어 충돌이 해결되고,
H2에서는 따옴표 없이 동작하여 기존 테스트가 깨지지 않는다.

**교훈:**
> 1. MySQL 예약어 목록(`YEAR_MONTH`, `RANK`, `KEY`, `ORDER` 등)은 컬럼명 지정 시 반드시 확인할 것.
> 2. Hibernate `globally_quoted_identifiers`는 DB별 따옴표 방식이 다르므로, 프로필별로 분리 설정해야 한다.
> 3. 에러가 발생하면 `try-catch`로 실제 메시지를 노출시키는 것이 디버깅의 첫 단계다.

---

## Security Issues

---

### Security #1: JWT Secret 하드코딩

**심각도:** CRITICAL | **상태:** Partially Resolved (환경변수 폴백으로 전환됨)

**문제:**
```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET:ZGV2LXNlY3JldC1rZXktZm9yLXByb2ZpdC1sb2dpYy1hcHBsaWNhdGlvbi0yMDI2}
```

Base64 디코딩 결과: `dev-secret-key-for-profit-logic-application-2026`

환경변수 `JWT_SECRET`이 설정되지 않으면 이 기본값이 사용된다.
**Git 히스토리에 이미 노출**되어 있으므로, 공격자가 이 값으로 JWT를 위조할 수 있다.

**해결 방법:**
1. 프로덕션에서 반드시 `JWT_SECRET` 환경변수를 별도로 설정
2. 기본값을 빈 문자열로 변경하고, 미설정 시 앱 시작 실패하도록 처리
3. 프로덕션 시크릿은 **최소 256비트(32바이트) 이상의 랜덤 값** 사용

```yaml
# 권장
jwt:
  secret: ${JWT_SECRET}  # 기본값 없음 — 미설정 시 시작 실패
```

**테스트 파일도 동일:**
- `application-test.yml` — 테스트 전용 시크릿 하드코딩 (테스트 환경이므로 허용)
- `JwtTokenProviderTest.java` — 동일한 dev 시크릿 사용

---

### Security #2: DB 크리덴셜 소스코드 노출

**심각도:** HIGH | **상태:** Partially Resolved

**문제:**
```yaml
# application.yml
datasource:
  username: ${SPRING_DATASOURCE_USERNAME:devuser}
  password: ${SPRING_DATASOURCE_PASSWORD:devpass}

# docker-compose.yml
MYSQL_ROOT_PASSWORD: root
MYSQL_USER: devuser
MYSQL_PASSWORD: devpass
```

개발용 크리덴셜이 소스코드에 그대로 노출. `docker-compose.yml`은 환경변수 치환 없이 하드코딩.

**해결 방법:**
1. `docker-compose.yml`에서 `.env` 파일 참조로 변경
2. `.env`를 `.gitignore`에 추가
3. 프로덕션 DB 크리덴셜은 GitHub Secrets → 환경변수로 주입

```yaml
# docker-compose.yml (권장)
environment:
  MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
  MYSQL_USER: ${MYSQL_USER}
  MYSQL_PASSWORD: ${MYSQL_PASSWORD}
```

---

### Security #3: Gemini API Key URL 쿼리 파라미터 노출

**심각도:** CRITICAL | **상태:** Open

**문제:**
```java
// GeminiClient.java:63
String uri = "/models/" + model + ":generateContent?key=" + config.getApiKey();
```

API Key가 **URL 쿼리 파라미터**로 전달된다.
- 서버 액세스 로그에 평문 기록
- 프록시/로드밸런서 로그에 노출
- Nginx access log에 전체 URL 기록

**해결 방법:**
Gemini API는 쿼리 파라미터 방식만 지원하므로 코드 변경은 불가하지만, 다음을 확인:
1. Nginx access log에서 쿼리 스트링 마스킹 설정
2. 프로덕션 환경변수 `GEMINI_API_KEY`가 GitHub Secrets로 관리되는지 확인
3. API Key 권한을 최소한으로 제한 (Google AI Studio에서 설정)

---

### Security #4: deploy.sh 평문 비밀키 포함

**심각도:** CRITICAL | **상태:** Resolved (커밋 `99b96d9`)

**문제:**
초기 `deploy.sh`에 JWT Secret, DB 패스워드 등이 평문으로 포함되어 있었다.
Git 히스토리에 남아있으므로 리포지토리가 공개되면 전체 크리덴셜 유출.

**해결:**
- 커밋 `99b96d9`: deploy.sh에서 하드코딩 시크릿 제거
- 커밋 `4db4f2f`: 환경변수로 외부화

**추가 조치 필요:**
- `git filter-branch` 또는 BFG Repo Cleaner로 히스토리에서 시크릿 완전 제거
- 또는 리포지토리를 Private으로 유지

---

### Security #5: CORS 와일드카드 + Credentials

**심각도:** HIGH | **상태:** Open

**문제:**
```java
// CorsConfig.java
configuration.setAllowedHeaders(List.of("*"));
configuration.setAllowCredentials(true);
```

`allowCredentials: true`와 `allowedHeaders: *`를 동시에 사용하면,
모든 헤더를 포함한 인증된 크로스 오리진 요청이 가능하다.

```yaml
# application.yml
cors:
  allowed-origins: http://localhost:5173  # 개발 서버만 허용
  allowed-headers: "*"                     # 모든 헤더 허용
  allow-credentials: true
```

**해결 방법:**
1. `allowed-headers`를 필요한 것만 명시: `Content-Type, Authorization`
2. 프로덕션 `allowed-origins`에 실제 도메인만 등록

```yaml
cors:
  allowed-origins: https://profitlogic.cloud
  allowed-headers: Content-Type,Authorization
  allow-credentials: true
```

---

### Security #6: CSRF 비활성화

**심각도:** MEDIUM | **상태:** Acceptable (JWT 사용)

**문제:**
```java
// SecurityConfig.java:29
.csrf(csrf -> csrf.disable())
```

CSRF 보호가 완전히 비활성화되어 있다.

**판단:**
JWT 토큰 기반 인증 + `SessionCreationPolicy.STATELESS` 조합에서는 CSRF 공격 벡터가 제한적.
쿠키 기반 세션이 아니므로 CSRF 비활성화는 **허용 가능**하지만, 문서화 필요.

**주의점:**
- JWT를 쿠키에 저장하는 방식으로 변경하면 CSRF를 반드시 활성화해야 한다
- 현재는 `Authorization` 헤더로 전송하므로 안전

---

### Security #7: H2 Console 인증 없이 노출

**심각도:** MEDIUM | **상태:** Open (로컬 환경만 해당)

**문제:**
```java
// SecurityConfig.java:33
.requestMatchers("/h2-console/**").permitAll()

// application-local.yml
h2.console.enabled: true
h2.console.path: /h2-console
```

H2 콘솔이 인증 없이 접근 가능. 로컬 개발 환경이므로 실제 위험도는 낮지만,
프로덕션 프로필에서 실수로 활성화되면 DB 전체 노출.

**해결 방법:**
1. `SecurityConfig`에서 프로필 분기 추가
2. 프로덕션 application.yml에 `h2.console.enabled: false` 명시
3. 또는 `@Profile("local")` 조건으로 H2 설정을 분리

---

### Security #8: DataInitializer 평문 패스워드 로깅

**심각도:** HIGH | **상태:** Resolved (커밋 `46f2b03`)

**문제:**
```java
// DataInitializer.java (이전 코드)
log.info("Admin account created: admin@profitlogic.com / Test1234!");
```

관리자 패스워드가 로그에 평문으로 출력되었다.
서버 로그 파일이 유출되면 관리자 계정 즉시 탈취 가능.

**해결:**
- 패스워드를 로그에서 제거
- 패스워드 복잡도 규칙 강화 (영문 + 숫자 + 특수문자)

---

### Security #9: Validation 에러 상세 노출

**심각도:** LOW | **상태:** Open

**문제:**
```java
// GlobalExceptionHandler.java:33-41
errors.stream().map(error ->
    error.getField() + ": " + error.getDefaultMessage())
```

유효성 검증 실패 시 필드명과 상세 메시지를 그대로 반환.
예: `"password: 8자 이상이어야 합니다"` → 공격자에게 비밀번호 정책 힌트 제공.

**해결 방법:**
```java
// 프로덕션에서는 제네릭 메시지 반환
return ResponseEntity.badRequest()
    .body(ErrorResponse.of("입력값이 올바르지 않습니다."));
```

또는 프로필 분기로 개발 환경에서만 상세 메시지를 반환하도록 처리.

---

## Security Checklist (프로덕션 배포 전)

| # | 항목 | 상태 |
|---|------|------|
| 1 | JWT Secret 환경변수로 주입 (기본값 제거) | Partially Done |
| 2 | DB 크리덴셜 `.env` 파일로 분리 + `.gitignore` | Partially Done |
| 3 | Git 히스토리에서 시크릿 제거 | Not Done |
| 4 | CORS `allowed-origins` 프로덕션 도메인으로 제한 | Not Done |
| 5 | CORS `allowed-headers` 와일드카드 제거 | Not Done |
| 6 | H2 Console 프로덕션에서 비활성화 확인 | Not Done |
| 7 | Nginx access log에서 API Key 마스킹 | Not Done |
| 8 | Validation 에러 메시지 제네릭화 | Not Done |
| 9 | `spring.jpa.show-sql: false` (프로덕션) | Done (prod profile) |
| 10 | HTTPS 강제 + HSTS 헤더 설정 | Not Verified |
