package com.wookidoki.profitlogic.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataInitializer의 30개 페르소나 시드 데이터를 활용한 시나리오 통합 테스트.
 * "local" 프로필을 함께 활성화하여 시드 데이터가 H2에 자동 생성됩니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "local"})
@DisplayName("페르소나 시나리오 통합 테스트 (시드 데이터 활용)")
class PersonaScenarioTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    private static final String SEED_PASSWORD = "Test1234!";

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private JsonNode parseData(ResponseEntity<String> response) throws Exception {
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.get("data");
    }

    private JsonNode parseRoot(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private String login(String email, String password) throws Exception {
        Map<String, Object> body = Map.of("email", email, "password", password);
        ResponseEntity<String> resp = restTemplate.exchange(url("/v1/auth/login"),
                HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return parseData(resp).get("access_token").asText();
    }

    // ══════════════════════════════════════════════════
    // Scenario A - 크리에이터 (웹소설 작가, creator01)
    // ══════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario A - 크리에이터 (웹소설 작가)")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ScenarioA_Creator {

        private String token;
        private Long projectId;

        @Test @Order(1)
        @DisplayName("A1. creator01 로그인 → JWT 획득")
        void login() throws Exception {
            Map<String, Object> body = Map.of("email", "creator01@test.com", "password", SEED_PASSWORD);
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/auth/login"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("access_token").asText()).isNotBlank();
            assertThat(data.get("email").asText()).isEqualTo("creator01@test.com");
            assertThat(data.get("nickname").asText()).isEqualTo("웹소설작가김");
            assertThat(data.get("role").asText()).isEqualTo("ROLE_USER");
            this.token = data.get("access_token").asText();
        }

        @Test @Order(2)
        @DisplayName("A2. 내 프로젝트 목록 → 1개 확인")
        void listProjects() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.isArray()).isTrue();
            assertThat(data.size()).isEqualTo(1);
            assertThat(data.get(0).get("title").asText()).isEqualTo("웹소설 연재 수익 분석");
            this.projectId = data.get(0).get("id").asLong();
        }

        @Test @Order(3)
        @DisplayName("A3. 프로젝트 상세 조회 → 가격/고정비 확인")
        void getProjectDetail() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/projects/" + projectId),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("price").decimalValue()).isEqualByComparingTo(new BigDecimal("3000"));
            assertThat(data.get("fixed_cost").decimalValue()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(data.get("variable_cost").decimalValue()).isEqualByComparingTo(new BigDecimal("100"));
            assertThat(data.get("work_hours").asInt()).isEqualTo(160);
        }

        @Test @Order(4)
        @DisplayName("A4. 프로젝트 분석 → BEP, 실질시급 검증")
        void analyzeProject() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/analysis"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);

            // BEP 확인
            JsonNode bep = data.get("bep");
            assertThat(bep).isNotNull();
            assertThat(bep.get("bep").decimalValue()).isPositive();
            assertThat(bep.get("contribution_margin").decimalValue()).isPositive();

            // 실질시급 확인
            JsonNode shadowWage = data.get("shadow_wage");
            assertThat(shadowWage).isNotNull();
            assertThat(shadowWage.get("real_shadow_wage").decimalValue()).isNotNull();

            // 액션카드 확인
            JsonNode actionCards = data.get("action_cards");
            assertThat(actionCards).isNotNull();
            assertThat(actionCards.isArray()).isTrue();
        }

        @Test @Order(5)
        @DisplayName("A5. 비용 목록 조회 → 2개 이상 (시드 데이터)")
        void listCosts() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/costs"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.isArray()).isTrue();
            // 최소 2개 (시드 기본) + 트렌드 데이터
            assertThat(data.size()).isGreaterThanOrEqualTo(2);
        }

        @Test @Order(6)
        @DisplayName("A6. 타임로그 목록 조회 → 3개 이상 (시드 데이터)")
        void listTimeLogs() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/timelogs"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.isArray()).isTrue();
            // 최소 3개 (시드 기본) + 트렌드 데이터
            assertThat(data.size()).isGreaterThanOrEqualTo(3);
        }

        @Test @Order(7)
        @DisplayName("A7. AI 챗봇 질문 → 응답 수신")
        void chat() throws Exception {
            Map<String, Object> body = Map.of(
                    "project_id", projectId,
                    "question", "이 프로젝트의 손익분기점이 궁금합니다"
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/chat"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("answer").asText()).isNotBlank();
            assertThat(data.get("question").asText()).contains("손익분기점");
        }

        @Test @Order(8)
        @DisplayName("A8. 대시보드 요약 → 프로젝트 1개 포함")
        void dashboardSummary() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/dashboard/summary"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("total_projects").asInt()).isEqualTo(1);
            assertThat(data.get("projects").isArray()).isTrue();
            assertThat(data.get("projects").size()).isEqualTo(1);
        }

        @Test @Order(9)
        @DisplayName("A9. 목표 진행률 조회")
        void goalProgress() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/goal"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("target_revenue").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(data.get("target_month").asText()).isEqualTo("2026-06");
        }

        @Test @Order(10)
        @DisplayName("A10. 월간 트렌드 조회 → 데이터 존재")
        void monthlyTrends() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/trends?months=6"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            // 응답: { months: [...], snapshots: [...] }
            assertThat(data.has("months")).isTrue();
            assertThat(data.has("snapshots")).isTrue();
            assertThat(data.get("snapshots").isArray()).isTrue();
            assertThat(data.get("snapshots").size()).isGreaterThanOrEqualTo(1);
        }

        @Test @Order(11)
        @DisplayName("A11. 타인 프로젝트 접근 → 403")
        void crossAccessForbidden() throws Exception {
            // seller01의 프로젝트 ID를 알아내기 위해 seller01 로그인
            String sellerToken = PersonaScenarioTest.this.login("seller01@test.com", SEED_PASSWORD);
            JsonNode sellerProjects = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(sellerToken)), String.class));
            Long sellerProjectId = sellerProjects.get(0).get("id").asLong();

            // creator01 토큰으로 seller01의 프로젝트 접근
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + sellerProjectId),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ══════════════════════════════════════════════════
    // Scenario B - 셀러 (핸드메이드 양초, seller01)
    // ══════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario B - 셀러 (핸드메이드 양초)")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ScenarioB_Seller {

        private String token;
        private Long projectId;
        private Long newCostId;
        private Long newTimeLogId;
        private Long postId;
        private Long commentId;

        @Test @Order(1)
        @DisplayName("B1. seller01 로그인")
        void login() throws Exception {
            this.token = PersonaScenarioTest.this.login("seller01@test.com", SEED_PASSWORD);
            assertThat(token).isNotBlank();
        }

        @Test @Order(2)
        @DisplayName("B2. 프로젝트 확인 + 분석 → BEP 검증 (양초: 변동비 높음)")
        void analyzeProject() throws Exception {
            JsonNode projects = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(projects.size()).isEqualTo(1);
            this.projectId = projects.get(0).get("id").asLong();

            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/analysis"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            JsonNode bep = data.get("bep");
            assertThat(bep.get("bep").decimalValue()).isPositive();
            // 양초: price=15000, variableCost=5000, fixedCost=500000
            // contribution_margin = 10000, BEP = 500000/10000 = 50
            assertThat(bep.get("contribution_margin").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test @Order(3)
        @DisplayName("B3. 비용 항목 추가 → 새 재료비 등록")
        void addCost() throws Exception {
            Map<String, Object> body = Map.of(
                    "category", "MATERIAL",
                    "cost_name", "새 향료 (라벤더)",
                    "cost_type", "VARIABLE",
                    "amount", 1500,
                    "memo", "신규 향 추가"
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/costs"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            this.newCostId = parseData(resp).get("id").asLong();
        }

        @Test @Order(4)
        @DisplayName("B4. 타임로그 추가 → 신규 작업시간")
        void addTimeLog() throws Exception {
            Map<String, Object> body = Map.of(
                    "task_name", "라벤더 캔들 시제품 제작",
                    "hours_spent", 4.0,
                    "log_date", "2026-02-12",
                    "memo", "새 향 테스트 배치"
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/timelogs"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            this.newTimeLogId = parseData(resp).get("id").asLong();
        }

        @Test @Order(5)
        @DisplayName("B5. 비용 목록 조회 → 추가한 항목 포함")
        void verifyCostAdded() throws Exception {
            JsonNode data = parseData(restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/costs"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));

            boolean found = false;
            for (JsonNode cost : data) {
                if (cost.get("id").asLong() == newCostId) {
                    assertThat(cost.get("cost_name").asText()).isEqualTo("새 향료 (라벤더)");
                    assertThat(cost.get("amount").decimalValue())
                            .isEqualByComparingTo(new BigDecimal("1500"));
                    found = true;
                    break;
                }
            }
            assertThat(found).isTrue();
        }

        @Test @Order(6)
        @DisplayName("B6. 게시글 작성 → 프로젝트 연결")
        void createPost() throws Exception {
            Map<String, Object> body = Map.of(
                    "title", "양초 셀러 비용 분석 후기",
                    "content", "재료비 추가 후 분석을 다시 돌려봤습니다. 원가 관리가 중요하네요!",
                    "project_id", projectId
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/community/posts"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            this.postId = parseData(resp).get("id").asLong();
        }

        @Test @Order(7)
        @DisplayName("B7. 댓글 작성 + 삭제")
        void commentCrud() throws Exception {
            // 댓글 작성
            Map<String, Object> body = Map.of("content", "좋은 분석이네요!");
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/community/posts/" + postId + "/comments"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            this.commentId = parseData(resp).get("id").asLong();

            // 댓글 삭제
            ResponseEntity<String> deleteResp = restTemplate.exchange(
                    url("/v1/community/comments/" + commentId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class);
            assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test @Order(8)
        @DisplayName("B8. 추가된 비용/시간 정리")
        void cleanup() throws Exception {
            restTemplate.exchange(url("/v1/costs/" + newCostId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class);
            restTemplate.exchange(url("/v1/timelogs/" + newTimeLogId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class);
            restTemplate.exchange(url("/v1/community/posts/" + postId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class);
        }
    }

    // ══════════════════════════════════════════════════
    // Scenario C - 개발자 (SaaS, dev01)
    // ══════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario C - 개발자 (SaaS)")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ScenarioC_Developer {

        private String token;
        private Long projectId;
        private Long newProjectId;

        @Test @Order(1)
        @DisplayName("C1. dev01 로그인")
        void login() throws Exception {
            this.token = PersonaScenarioTest.this.login("dev01@test.com", SEED_PASSWORD);
            assertThat(token).isNotBlank();
        }

        @Test @Order(2)
        @DisplayName("C2. 프로젝트 분석 → BEP 검증 (SaaS: 고정비 높음)")
        void analyzeProject() throws Exception {
            JsonNode projects = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(projects.size()).isEqualTo(1);
            this.projectId = projects.get(0).get("id").asLong();

            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/analysis"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            JsonNode bep = data.get("bep");
            assertThat(bep).isNotNull();
            // CostDetail 합산으로 BEP 계산 (시드 비용 포함)
            assertThat(bep.get("bep").decimalValue()).isPositive();
            assertThat(bep.get("price").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(bep.get("contribution_margin").decimalValue()).isPositive();
            assertThat(bep.get("enhanced_fixed_cost").decimalValue()).isPositive();
        }

        @Test @Order(3)
        @DisplayName("C3. 가격 시뮬레이션 → 가격 인상 시 수익 변화 확인")
        void priceSimulation() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/analysis/price-simulation?newPrice=70000"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("current_price").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(data.get("new_price").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("70000"));
            // 가격 인상 → 양수 변화율
            assertThat(data.get("price_change_rate").decimalValue()).isPositive();
        }

        @Test @Order(4)
        @DisplayName("C4. 새 프로젝트 생성 → 두 번째 프로젝트")
        void createSecondProject() throws Exception {
            Map<String, Object> body = Map.of(
                    "title", "모바일 앱 사이드 프로젝트",
                    "price", 5000, "variable_cost", 500, "fixed_cost", 800000,
                    "work_hours", 80, "hourly_wage", 25000, "is_public", false
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            this.newProjectId = parseData(resp).get("id").asLong();
        }

        @Test @Order(5)
        @DisplayName("C5. 프로젝트 목록 → 2개 확인")
        void listProjects() throws Exception {
            JsonNode data = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(data.size()).isEqualTo(2);
        }

        @Test @Order(6)
        @DisplayName("C6. 대시보드 → 2개 프로젝트 요약")
        void dashboard() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/dashboard/summary"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("total_projects").asInt()).isEqualTo(2);
        }

        @Test @Order(7)
        @DisplayName("C7. 목표 수정 → 목표 변경 확인")
        void updateGoal() throws Exception {
            Map<String, Object> body = Map.of(
                    "target_revenue", 15000000,
                    "target_month", "2027-06"
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/goal"),
                    HttpMethod.PUT, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

            // 변경 확인
            JsonNode goalData = parseData(restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/goal"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(goalData.get("target_revenue").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("15000000"));
            assertThat(goalData.get("target_month").asText()).isEqualTo("2027-06");
        }

        @Test @Order(8)
        @DisplayName("C8. 리소스 정리 → 새 프로젝트 삭제")
        void cleanup() throws Exception {
            restTemplate.exchange(url("/v1/projects/" + newProjectId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class);

            // 원래 목표 복원
            Map<String, Object> body = Map.of(
                    "target_revenue", 10000000,
                    "target_month", "2026-12"
            );
            restTemplate.exchange(url("/v1/projects/" + projectId + "/goal"),
                    HttpMethod.PUT, new HttpEntity<>(body, authHeaders(token)), String.class);
        }
    }

    // ══════════════════════════════════════════════════
    // Scenario D - 관리자
    // ══════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario D - 관리자")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ScenarioD_Admin {

        private String adminToken;
        private String userToken;

        @Test @Order(1)
        @DisplayName("D1. 관리자 로그인 → role=ROLE_ADMIN 확인")
        void adminLogin() throws Exception {
            Map<String, Object> body = Map.of(
                    "email", "admin@profitlogic.com", "password", SEED_PASSWORD
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/auth/login"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("role").asText()).isEqualTo("ROLE_ADMIN");
            this.adminToken = data.get("access_token").asText();
        }

        @Test @Order(2)
        @DisplayName("D2. 관리자 통계 API → 유저 31명 이상 (관리자+30페르소나)")
        void adminStats() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/admin/stats"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(adminToken)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("user_count").asInt()).isGreaterThanOrEqualTo(31);
            assertThat(data.get("project_count").asInt()).isGreaterThanOrEqualTo(31);
            assertThat(data.get("post_count").asInt()).isGreaterThanOrEqualTo(2);
            assertThat(data.get("comment_count").asInt()).isGreaterThanOrEqualTo(2);
        }

        @Test @Order(3)
        @DisplayName("D3. 사용자 목록 조회 → 전체 리스트")
        void adminUserList() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/admin/users"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(adminToken)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.isArray()).isTrue();
            assertThat(data.size()).isGreaterThanOrEqualTo(31);

            // 관리자 계정이 포함되어 있는지 확인
            boolean adminFound = false;
            for (JsonNode user : data) {
                if ("admin@profitlogic.com".equals(user.get("email").asText())) {
                    assertThat(user.get("role").asText()).isEqualTo("ROLE_ADMIN");
                    adminFound = true;
                    break;
                }
            }
            assertThat(adminFound).isTrue();
        }

        @Test @Order(4)
        @DisplayName("D4. 일반 사용자가 관리자 API 호출 → 403")
        void userCannotAccessAdminApi() throws Exception {
            this.userToken = PersonaScenarioTest.this.login("creator01@test.com", SEED_PASSWORD);

            ResponseEntity<String> statsResp = restTemplate.exchange(url("/v1/admin/stats"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(userToken)), String.class);
            assertThat(statsResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

            ResponseEntity<String> usersResp = restTemplate.exchange(url("/v1/admin/users"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(userToken)), String.class);
            assertThat(usersResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test @Order(5)
        @DisplayName("D5. 미인증 상태에서 관리자 API → 401/403")
        void unauthenticatedCannotAccessAdminApi() {
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/admin/stats"),
                    HttpMethod.GET, new HttpEntity<>(jsonHeaders()), String.class);
            assertThat(resp.getStatusCode().value()).isIn(401, 403);
        }
    }

    // ══════════════════════════════════════════════════
    // Scenario E - 다양한 페르소나 로그인 검증
    // ══════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario E - 전체 페르소나 로그인 검증")
    class ScenarioE_AllPersonaLogin {

        @Test
        @DisplayName("E1. 크리에이터 10명 전원 로그인 가능")
        void allCreatorsCanLogin() throws Exception {
            for (int i = 1; i <= 10; i++) {
                String email = String.format("creator%02d@test.com", i);
                String token = PersonaScenarioTest.this.login(email, SEED_PASSWORD);
                assertThat(token).as("creator%02d 로그인 실패", i).isNotBlank();
            }
        }

        @Test
        @DisplayName("E2. 셀러 10명 전원 로그인 가능")
        void allSellersCanLogin() throws Exception {
            for (int i = 1; i <= 10; i++) {
                String email = String.format("seller%02d@test.com", i);
                String token = PersonaScenarioTest.this.login(email, SEED_PASSWORD);
                assertThat(token).as("seller%02d 로그인 실패", i).isNotBlank();
            }
        }

        @Test
        @DisplayName("E3. 개발자 10명 전원 로그인 가능")
        void allDevelopersCanLogin() throws Exception {
            for (int i = 1; i <= 10; i++) {
                String email = String.format("dev%02d@test.com", i);
                String token = PersonaScenarioTest.this.login(email, SEED_PASSWORD);
                assertThat(token).as("dev%02d 로그인 실패", i).isNotBlank();
            }
        }

        @Test
        @DisplayName("E4. 모든 페르소나가 각각 1개 프로젝트 보유")
        void allPersonasHaveOneProject() throws Exception {
            String[] prefixes = {"creator", "seller", "dev"};
            for (String prefix : prefixes) {
                for (int i = 1; i <= 10; i++) {
                    String email = String.format("%s%02d@test.com", prefix, i);
                    String token = PersonaScenarioTest.this.login(email, SEED_PASSWORD);
                    JsonNode data = parseData(restTemplate.exchange(url("/v1/projects"),
                            HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
                    assertThat(data.size()).as("%s 프로젝트 없음", email).isEqualTo(1);
                }
            }
        }
    }

    // ══════════════════════════════════════════════════
    // Scenario F - 교차 보안 (시드 페르소나 간)
    // ══════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario F - 교차 보안 (페르소나 간)")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ScenarioF_CrossSecurity {

        private String creatorToken;
        private String sellerToken;
        private String devToken;
        private Long creatorProjectId;
        private Long sellerProjectId;
        private Long devProjectId;

        @Test @Order(1)
        @DisplayName("F1. 3명 로그인 + 프로젝트 ID 확보")
        void setup() throws Exception {
            creatorToken = PersonaScenarioTest.this.login("creator01@test.com", SEED_PASSWORD);
            sellerToken = PersonaScenarioTest.this.login("seller01@test.com", SEED_PASSWORD);
            devToken = PersonaScenarioTest.this.login("dev01@test.com", SEED_PASSWORD);

            creatorProjectId = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(creatorToken)), String.class))
                    .get(0).get("id").asLong();
            sellerProjectId = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(sellerToken)), String.class))
                    .get(0).get("id").asLong();
            devProjectId = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(devToken)), String.class))
                    .get(0).get("id").asLong();
        }

        @Test @Order(2)
        @DisplayName("F2. Creator → Seller 프로젝트 접근 차단")
        void creatorCannotAccessSellerProject() {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + sellerProjectId),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(creatorToken)), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test @Order(3)
        @DisplayName("F3. Seller → Developer 프로젝트 분석 접근 차단")
        void sellerCannotAccessDevAnalysis() {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + devProjectId + "/analysis"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(sellerToken)), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test @Order(4)
        @DisplayName("F4. Developer → Creator 프로젝트에 챗봇 질문 차단")
        void devCannotChatOnCreatorProject() {
            Map<String, Object> body = Map.of(
                    "project_id", creatorProjectId,
                    "question", "이 프로젝트 정보 알려줘"
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/chat"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(devToken)), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test @Order(5)
        @DisplayName("F5. Creator → Seller 프로젝트에 비용 추가 차단")
        void creatorCannotAddCostToSellerProject() {
            Map<String, Object> body = Map.of(
                    "category", "MATERIAL",
                    "cost_name", "침입 비용",
                    "cost_type", "VARIABLE",
                    "amount", 999
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + sellerProjectId + "/costs"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(creatorToken)), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
