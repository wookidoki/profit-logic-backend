# Profit Logic — Backend API

> 1인 크리에이터를 위한 AI 수익성 분석 서비스의 백엔드 API 서버

🔗 **배포 URL:** http://profitlogic.cloud
📦 **Frontend:** [profit-logic-frontend](https://github.com/wookidoki/profit-logic-frontend)

---

## 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 21 | 메인 언어 |
| Spring Boot | 3.5 | 프레임워크 |
| Spring Security | 6.x | JWT 기반 인증/인가 |
| Spring Data JPA | 3.x | ORM + 쿼리 |
| Spring WebFlux | 6.x | Gemini API 비동기 호출 (논블로킹) |
| MySQL | 8.0 | 운영 DB |
| H2 | — | 개발/테스트 인메모리 DB |
| Google Gemini API | 2.5 | AI 챗봇 (Flash) + 리포트 (Pro) |
| Docker Compose | — | 컨테이너 오케스트레이션 |
| GitHub Actions | — | CI/CD |
| JUnit 5 + Mockito | — | 테스트 (300+) |

---

## 핵심 기능

| 기능 | 설명 |
|------|------|
| **수익성 분석 엔진** | BEP(월 최소 건수), 실질 시급, 건당 순수익, 안전마진율 — BigDecimal 기반 정밀 계산 |
| **AI 채팅 상담** | Gemini API + 규칙 기반 14패턴 이중 시스템 (LLM 실패 시 자동 폴백) |
| **맞춤 상담 스크립트** | 크리에이터 5유형별 단계적 질문 → 프로젝트 자동 생성 |
| **비용/시간 관리** | 카테고리별 비용 기록 + 일별 작업시간 트래킹 + CSV 일괄 업로드 |
| **가격 시뮬레이션** | 단가/비용 변경 시 BEP·수익 변화 즉시 비교 |
| **AI 리포트** | Gemini Pro 기반 월간 종합 분석 리포트 자동 생성 |
| **커뮤니티** | 게시글/댓글 CRUD |
| **관리자** | 서비스 통계, 사용자/게시글 관리 (ROLE_ADMIN) |

---

## 실행 방법

### 사전 요구사항
- Java 21+
- Docker (MySQL 사용 시)

### 로컬 개발 (H2 인메모리)
```bash
git clone https://github.com/wookidoki/profit-logic-backend.git
cd profit-logic-backend
./gradlew bootRun
# → http://localhost:8080/api/health
```

### Docker MySQL 환경
```bash
docker compose up -d          # MySQL 컨테이너 시작
./gradlew bootRun             # Spring Boot 시작
```

### 빌드 & 테스트
```bash
./gradlew clean build         # 빌드 + 300+ 테스트 실행
./gradlew test                # 테스트만
```

---

## 프로젝트 구조

```
src/main/java/com/wookidoki/profitlogic/
├── config/                    # 설정
│   ├── SecurityConfig         # Spring Security + JWT 필터
│   ├── CorsConfig             # CORS 설정
│   ├── LlmProperties          # Gemini API 설정 (yaml 바인딩)
│   ├── WebClientConfig         # WebFlux WebClient 빈
│   ├── DataInitializer         # 시드 데이터 (33개 페르소나)
│   └── jwt/
│       ├── JwtTokenProvider    # 토큰 생성/검증
│       └── JwtAuthenticationFilter
│
├── controller/                # REST API (18개)
│   ├── AuthController          # 회원가입, 로그인
│   ├── ProjectController       # 프로젝트 CRUD
│   ├── ChatController          # AI 채팅
│   ├── AnalysisController      # 수익성 분석
│   ├── SimulationController    # 가격 시뮬레이션
│   ├── CostDetailController    # 비용 상세 (+ CSV 업로드)
│   ├── TimeLogController       # 작업시간 기록
│   ├── ScriptController        # 맞춤 상담 스크립트
│   ├── ReportController        # AI 리포트
│   ├── DashboardController     # 대시보드 요약
│   ├── TrendController         # 월별 추이
│   ├── GoalController          # 목표 추적
│   ├── CommunityController     # 게시판
│   ├── AdminController         # 관리자
│   ├── MyPageController        # 마이페이지
│   └── HealthController        # 헬스체크
│
├── service/                   # 비즈니스 로직 (20개)
│   ├── ChatService             # AI 채팅 (LLM + 규칙 기반 이중 시스템)
│   ├── CalculationService      # 수익성 분석 오케스트레이터
│   ├── CsvCostParseService     # CSV 파싱 → 비용 일괄 등록
│   ├── ScriptTemplateProvider  # 크리에이터 유형별 스크립트 템플릿
│   └── ...
│
├── domain/                    # JPA 엔티티 (16개)
│   ├── User, Project, ChatLog, CostDetail, TimeLog
│   ├── BoardPost, Comment, Report, Simulation, DailyLog
│   ├── CreatorCategory, CostCategory, BizType, Role (enum)
│   └── logic/
│       └── FinancialCalculator # 순수 계산 로직 (BigDecimal)
│
├── dto/                       # 요청/응답 DTO (15개 하위 패키지)
│   ├── auth/       LoginRequest, LoginResponse, SignupRequest
│   ├── project/    ProjectCreateRequest, ProjectResponse, ...
│   ├── chat/       ChatRequest, ChatResponse
│   ├── finance/    CalculateRequest, CalculateResponse, BepDto, ...
│   ├── cost/       CostDetailCreateRequest, CsvUploadResponse
│   ├── script/     ScriptTemplate, ScriptAnalysisRequest/Response
│   └── ...
│
├── repository/                # Spring Data JPA (11개)
│
├── client/                    # 외부 API 클라이언트
│   ├── LlmClient (interface)  # LLM 추상화
│   ├── GeminiClient            # Gemini API 구현체 (WebFlux)
│   ├── PromptTemplates         # 시스템 프롬프트 상수
│   └── dto/                    # Gemini API 요청/응답
│
└── common/                    # 공통
    ├── GlobalExceptionHandler  # 통일 에러 응답
    ├── ResponseData<T>         # 공통 응답 래퍼
    └── exception/              # 커스텀 예외 5종
```

---

## API 엔드포인트 (35개)

| 분류 | 메서드 | 경로 | 인증 |
|------|--------|------|------|
| **인증** | POST | `/api/v1/auth/signup` | — |
| | POST | `/api/v1/auth/login` | — |
| **프로젝트** | GET/POST | `/api/v1/projects` | JWT |
| | GET/PUT/DELETE | `/api/v1/projects/{id}` | JWT |
| **분석** | GET | `/api/v1/projects/{id}/analysis` | JWT |
| **시뮬레이션** | POST/GET | `/api/v1/projects/{id}/simulations` | JWT |
| **AI 채팅** | POST | `/api/v1/chat` | JWT |
| | GET | `/api/v1/chat/history/{projectId}` | JWT |
| **비용** | GET/POST/DELETE | `/api/v1/projects/{id}/costs` | JWT |
| | POST | `/api/v1/projects/{id}/costs/csv` | JWT |
| **시간** | GET/POST/DELETE | `/api/v1/projects/{id}/timelogs` | JWT |
| **리포트** | POST/GET | `/api/v1/projects/{id}/reports` | JWT |
| **스크립트** | GET/POST | `/api/v1/scripts/*` | — |
| **커뮤니티** | CRUD | `/api/v1/posts/*` | 일부 JWT |
| **관리자** | GET/DELETE | `/api/v1/admin/*` | ADMIN |

---

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `GEMINI_API_KEY` | Google Gemini API 키 (없으면 규칙 기반 폴백) | — |
| `JWT_SECRET` | JWT 서명 키 | 개발용 기본값 |
| `SPRING_DATASOURCE_URL` | DB 접속 URL | H2 인메모리 |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자 | sa |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | — |

---

## 시드 데이터

개발 환경에서 `DataInitializer`가 자동으로 33개 페르소나 + 샘플 데이터를 생성합니다.

| 계정 | 이메일 | 비밀번호 | 역할 |
|------|--------|---------|------|
| 데모 | `demo@profitlogic.com` | `demo1234` | USER |
| 관리자 | `admin@profitlogic.com` | `admin1234` | ADMIN |
| 페르소나 | `novel_1@test.com` ~ `indie_6@test.com` | `test1234` | USER |

5개 크리에이터 유형 × 6명 + 데모 + 관리자 + 기본 = 33계정

---

## 테스트 (300+)

| 파일 | 대상 |
|------|------|
| `FinancialCalculatorTest` | BEP/시급/순수익 계산 정확성 + 엣지 케이스 (0 나누기 등) |
| `ChatServiceTest` | 규칙 기반 14패턴 매칭 + LLM 폴백 |
| `PersonaE2eIntegrationTest` | 33개 페르소나별 E2E 시나리오 |
| `FullFeatureIntegrationTest` | 전체 기능 통합 테스트 (MockMvc) |
| `AuthServiceTest` | 회원가입/로그인/JWT |
| `ProjectServiceTest` | CRUD + 권한 검증 |
| `GeminiClientTest` | API 호출 + 에러 핸들링 |
| ... | 총 18개 테스트 파일 |

---

## 배포 아키텍처

```
AWS EC2 (Ubuntu 24.04)
├── Docker Compose
│   ├── Nginx (포트 80)        → 정적 파일 서빙 + /api/ 리버스 프록시
│   ├── Spring Boot (포트 8080) → API 서버
│   └── MySQL 8.0 (포트 3306)  → Docker Volume 영속
│
├── GitHub Actions
│   ├── CI: PR → gradlew test
│   └── CD: develop merge → SSH → 자동 배포
```
