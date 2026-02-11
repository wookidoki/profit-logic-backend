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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("페르소나 E2E 통합 테스트")
class PersonaE2eIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

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
        return objectMapper.readTree(response.getBody()).get("data");
    }

    private JsonNode parseRoot(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    // ────────────────────────────────────────────────
    // Helper: 회원가입 + 로그인 → 토큰 반환
    // ────────────────────────────────────────────────
    private String signupAndLogin(String email, String password, String nickname) throws Exception {
        // Signup
        Map<String, Object> signupBody = Map.of(
                "email", email, "password", password, "nickname", nickname
        );
        restTemplate.exchange(url("/v1/auth/signup"), HttpMethod.POST,
                new HttpEntity<>(signupBody, jsonHeaders()), String.class);

        // Login
        Map<String, Object> loginBody = Map.of("email", email, "password", password);
        ResponseEntity<String> loginResp = restTemplate.exchange(url("/v1/auth/login"),
                HttpMethod.POST, new HttpEntity<>(loginBody, jsonHeaders()), String.class);
        return parseData(loginResp).get("access_token").asText();
    }

    // ══════════════════════════════════════════════════
    // Type A - 시간 투자형 (웹소설 작가, CREATOR)
    // ══════════════════════════════════════════════════
    @Nested
    @DisplayName("Type A - 시간 투자형 (웹소설 작가)")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TypeA_Creator {

        private String token;
        private Long projectId;
        private Long postId;
        private Long commentId;
        private Long simulationId;

        static final String EMAIL = "creator@example.com";
        static final String PASSWORD = "Creator1234!";
        static final String NICKNAME = "웹소설작가김";

        @Test @Order(1)
        @DisplayName("1. 회원가입 성공")
        void signup() throws Exception {
            Map<String, Object> body = Map.of(
                    "email", EMAIL, "password", PASSWORD, "nickname", NICKNAME
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/auth/signup"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            JsonNode root = parseRoot(resp);
            assertThat(root.get("success").asBoolean()).isTrue();
            assertThat(root.get("message").asText()).contains("회원가입");
        }

        @Test @Order(2)
        @DisplayName("2. 로그인 및 JWT 토큰 발급")
        void login() throws Exception {
            Map<String, Object> body = Map.of("email", EMAIL, "password", PASSWORD);
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/auth/login"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("access_token").asText()).isNotBlank();
            assertThat(data.get("email").asText()).isEqualTo(EMAIL);
            assertThat(data.get("nickname").asText()).isEqualTo(NICKNAME);
            this.token = data.get("access_token").asText();
        }

        @Test @Order(3)
        @DisplayName("3. 프로젝트 생성 - 웹소설 연재")
        void createProject() throws Exception {
            Map<String, Object> body = Map.of(
                    "title", "웹소설 연재 프로젝트",
                    "price", 3000, "variable_cost", 100, "fixed_cost", 50000,
                    "work_hours", 160, "hourly_wage", 9860, "is_public", false
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            JsonNode data = parseData(resp);
            assertThat(data.get("title").asText()).isEqualTo("웹소설 연재 프로젝트");
            this.projectId = data.get("id").asLong();
            assertThat(this.projectId).isPositive();
        }

        @Test @Order(4)
        @DisplayName("4. 내 프로젝트 목록 조회")
        void getMyProjects() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.isArray()).isTrue();
            assertThat(data.size()).isEqualTo(1);
            assertThat(data.get(0).get("title").asText()).isEqualTo("웹소설 연재 프로젝트");
        }

        @Test @Order(5)
        @DisplayName("5. 손익분석 - BEP=18, shadowWage=12500.06, isViable=true")
        void calculate() throws Exception {
            Map<String, Object> body = Map.of(
                    "price", 3000, "variable_cost", 100, "fixed_cost", 50000,
                    "work_hours", 160, "hourly_wage", 9860, "target_profit", 2000000
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);

            assertThat(data.get("break_even_point").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("18"));
            assertThat(data.get("contribution_margin").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("2900"));
            assertThat(data.get("target_quantity").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("706.90"));
            assertThat(data.get("operating_profit").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("2000010.00"));
            assertThat(data.get("economic_profit").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("422410.00"));
            assertThat(data.get("shadow_wage").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("12500.06"));
            assertThat(data.get("margin_rate").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("97.45"));
            assertThat(data.get("is_viable").asBoolean()).isTrue();
        }

        @Test @Order(6)
        @DisplayName("6. 커뮤니티 게시글 작성")
        void createPost() throws Exception {
            Map<String, Object> body = Map.of(
                    "title", "웹소설 작가의 손익분석 후기",
                    "content", "BEP가 18회분! 생각보다 적은 양으로 손익분기점 도달 가능하네요.",
                    "project_id", projectId
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/community/posts"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            JsonNode data = parseData(resp);
            this.postId = data.get("id").asLong();
            assertThat(data.get("author_nickname").asText()).isEqualTo(NICKNAME);
        }

        @Test @Order(7)
        @DisplayName("7. 댓글 작성")
        void createComment() throws Exception {
            Map<String, Object> body = Map.of("content", "저도 웹소설 작가인데 공감됩니다!");
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/community/posts/" + postId + "/comments"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            JsonNode data = parseData(resp);
            this.commentId = data.get("id").asLong();
            assertThat(data.get("content").asText()).contains("웹소설 작가");
        }

        @Test @Order(8)
        @DisplayName("8. 챗봇 질문 - 손익분기점")
        void chat() throws Exception {
            Map<String, Object> body = Map.of(
                    "project_id", projectId,
                    "question", "이 프로젝트의 손익분기점이 궁금합니다"
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/chat"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("answer").asText()).contains("웹소설 연재 프로젝트");
            assertThat(data.get("question").asText()).contains("손익분기점");
        }

        @Test @Order(9)
        @DisplayName("9. 시뮬레이션 저장")
        void saveSimulation() throws Exception {
            Map<String, Object> body = Map.of(
                    "project_id", projectId,
                    "scenario_name", "기본 시나리오",
                    "result_json", "{\"bep\":18,\"target_quantity\":706.90}"
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/simulations"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            JsonNode data = parseData(resp);
            this.simulationId = data.get("id").asLong();
            assertThat(data.get("scenario_name").asText()).isEqualTo("기본 시나리오");
        }

        @Test @Order(10)
        @DisplayName("10. 시뮬레이션 조회")
        void getSimulations() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/simulations/project/" + projectId),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.isArray()).isTrue();
            assertThat(data.size()).isEqualTo(1);
        }

        @Test @Order(11)
        @DisplayName("11. 리소스 정리")
        void cleanup() throws Exception {
            assertThat(restTemplate.exchange(url("/v1/simulations/" + simulationId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class)
                    .getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(restTemplate.exchange(url("/v1/community/comments/" + commentId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class)
                    .getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(restTemplate.exchange(url("/v1/community/posts/" + postId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class)
                    .getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(restTemplate.exchange(url("/v1/projects/" + projectId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class)
                    .getStatusCode()).isEqualTo(HttpStatus.OK);

            // 프로젝트 목록 비어있는지 확인
            JsonNode data = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(data.size()).isEqualTo(0);
        }
    }

    // ══════════════════════════════════════════════════
    // Type B - 비용 지출형 (쇼핑몰 셀러, SELLER)
    // ══════════════════════════════════════════════════
    @Nested
    @DisplayName("Type B - 비용 지출형 (쇼핑몰 셀러)")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TypeB_Seller {

        private String token;
        private Long projectId;
        private Long postId;
        private Long commentId;
        private Long simulationId;

        static final String EMAIL = "seller@example.com";
        static final String PASSWORD = "Seller12345!";
        static final String NICKNAME = "쇼핑몰셀러박";

        @Test @Order(1)
        @DisplayName("1. 회원가입 성공")
        void signup() throws Exception {
            Map<String, Object> body = Map.of(
                    "email", EMAIL, "password", PASSWORD, "nickname", NICKNAME
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/auth/signup"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test @Order(2)
        @DisplayName("2. 로그인 및 JWT 토큰 발급")
        void login() throws Exception {
            Map<String, Object> body = Map.of("email", EMAIL, "password", PASSWORD);
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/auth/login"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);
            this.token = parseData(resp).get("access_token").asText();
            assertThat(this.token).isNotBlank();
        }

        @Test @Order(3)
        @DisplayName("3. 프로젝트 생성 - 쇼핑몰 운영")
        void createProject() throws Exception {
            Map<String, Object> body = Map.of(
                    "title", "쇼핑몰 운영 프로젝트",
                    "price", 25000, "variable_cost", 12000, "fixed_cost", 1500000,
                    "work_hours", 200, "hourly_wage", 9860, "is_public", false
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            this.projectId = parseData(resp).get("id").asLong();
        }

        @Test @Order(4)
        @DisplayName("4. 내 프로젝트 목록 조회")
        void getMyProjects() throws Exception {
            JsonNode data = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(data.size()).isEqualTo(1);
            assertThat(data.get(0).get("title").asText()).isEqualTo("쇼핑몰 운영 프로젝트");
        }

        @Test @Order(5)
        @DisplayName("5. 손익분석 - BEP=116, shadowWage=25000, isViable=true")
        void calculate() throws Exception {
            Map<String, Object> body = Map.of(
                    "price", 25000, "variable_cost", 12000, "fixed_cost", 1500000,
                    "work_hours", 200, "hourly_wage", 9860, "target_profit", 5000000
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);

            assertThat(data.get("break_even_point").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("116"));
            assertThat(data.get("contribution_margin").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("13000"));
            assertThat(data.get("target_quantity").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(data.get("operating_profit").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("5000000.00"));
            assertThat(data.get("economic_profit").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("3028000.00"));
            assertThat(data.get("shadow_wage").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("25000.00"));
            assertThat(data.get("margin_rate").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("76.80"));
            assertThat(data.get("is_viable").asBoolean()).isTrue();
        }

        @Test @Order(6)
        @DisplayName("6. 커뮤니티 게시글 작성")
        void createPost() throws Exception {
            Map<String, Object> body = Map.of(
                    "title", "쇼핑몰 셀러의 BEP 분석 공유",
                    "content", "월 116개만 팔면 손익분기점 달성! 공헌이익 13,000원이 핵심입니다.",
                    "project_id", projectId
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/community/posts"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            this.postId = parseData(resp).get("id").asLong();
        }

        @Test @Order(7)
        @DisplayName("7. 댓글 작성")
        void createComment() throws Exception {
            Map<String, Object> body = Map.of("content", "셀러분 공헌이익 분석 감사합니다!");
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/community/posts/" + postId + "/comments"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            this.commentId = parseData(resp).get("id").asLong();
        }

        @Test @Order(8)
        @DisplayName("8. 챗봇 질문 - 수익성")
        void chat() throws Exception {
            Map<String, Object> body = Map.of(
                    "project_id", projectId,
                    "question", "이 쇼핑몰의 수익성은 어떤가요?"
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/chat"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("answer").asText()).contains("쇼핑몰 운영 프로젝트");
        }

        @Test @Order(9)
        @DisplayName("9. 시뮬레이션 저장")
        void saveSimulation() throws Exception {
            Map<String, Object> body = Map.of(
                    "project_id", projectId,
                    "scenario_name", "원가 절감 시나리오",
                    "result_json", "{\"bep\":116,\"target_quantity\":500}"
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/simulations"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            this.simulationId = parseData(resp).get("id").asLong();
        }

        @Test @Order(10)
        @DisplayName("10. 시뮬레이션 조회")
        void getSimulations() throws Exception {
            JsonNode data = parseData(restTemplate.exchange(
                    url("/v1/simulations/project/" + projectId),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(data.size()).isEqualTo(1);
        }

        @Test @Order(11)
        @DisplayName("11. 리소스 정리")
        void cleanup() throws Exception {
            restTemplate.exchange(url("/v1/simulations/" + simulationId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class);
            restTemplate.exchange(url("/v1/community/comments/" + commentId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class);
            restTemplate.exchange(url("/v1/community/posts/" + postId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class);
            restTemplate.exchange(url("/v1/projects/" + projectId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class);

            JsonNode data = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(data.size()).isEqualTo(0);
        }
    }

    // ══════════════════════════════════════════════════
    // 교차 보안 검증
    // ══════════════════════════════════════════════════
    @Nested
    @DisplayName("교차 보안 검증 - 타 사용자 리소스 접근 차단")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class CrossPersonaSecurity {

        private String tokenX;
        private String tokenY;
        private Long projectIdX;

        @Test @Order(1)
        @DisplayName("1. 두 사용자 생성 및 UserX 프로젝트 생성")
        void setup() throws Exception {
            tokenX = signupAndLogin("security-x@test.com", "Password1234!", "유저X");
            tokenY = signupAndLogin("security-y@test.com", "Password1234!", "유저Y");

            Map<String, Object> body = Map.of(
                    "title", "X의 비밀 프로젝트",
                    "price", 10000, "variable_cost", 3000, "fixed_cost", 500000,
                    "work_hours", 100, "hourly_wage", 9860, "is_public", false
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(tokenX)), String.class);
            this.projectIdX = parseData(resp).get("id").asLong();
        }

        @Test @Order(2)
        @DisplayName("2. UserY → UserX 프로젝트 상세 조회 시 403")
        void crossAccessProject() {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectIdX),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(tokenY)), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test @Order(3)
        @DisplayName("3. UserY → UserX 프로젝트에 시뮬레이션 저장 시 403")
        void crossAccessSimulation() {
            Map<String, Object> body = Map.of(
                    "project_id", projectIdX,
                    "scenario_name", "침입 시도",
                    "result_json", "{}"
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/simulations"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(tokenY)), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test @Order(4)
        @DisplayName("4. UserY → UserX 프로젝트에 챗봇 질문 시 403")
        void crossAccessChat() {
            Map<String, Object> body = Map.of(
                    "project_id", projectIdX,
                    "question", "이 프로젝트 정보 알려줘"
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/chat"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(tokenY)), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ══════════════════════════════════════════════════
    // 공개 커뮤니티 접근 검증
    // ══════════════════════════════════════════════════
    @Nested
    @DisplayName("공개 커뮤니티 접근 검증")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class PublicCommunityAccess {

        private String token;
        private Long postId;

        @Test @Order(1)
        @DisplayName("1. 인증 사용자가 게시글 + 댓글 생성 (setup)")
        void setup() throws Exception {
            token = signupAndLogin("community@test.com", "Password1234!", "커뮤니티유저");

            Map<String, Object> postBody = Map.of(
                    "title", "공개 게시글", "content", "누구나 볼 수 있는 글입니다"
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/community/posts"),
                    HttpMethod.POST, new HttpEntity<>(postBody, authHeaders(token)), String.class);
            this.postId = parseData(resp).get("id").asLong();

            Map<String, Object> commentBody = Map.of("content", "첫 댓글입니다");
            restTemplate.exchange(url("/v1/community/posts/" + postId + "/comments"),
                    HttpMethod.POST, new HttpEntity<>(commentBody, authHeaders(token)), String.class);
        }

        @Test @Order(2)
        @DisplayName("2. 미인증 - 게시글 목록 조회 가능 (200)")
        void unauthenticatedCanListPosts() {
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/community/posts"),
                    HttpMethod.GET, new HttpEntity<>(jsonHeaders()), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test @Order(3)
        @DisplayName("3. 미인증 - 게시글 상세 조회 가능 (200)")
        void unauthenticatedCanGetPost() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/community/posts/" + postId),
                    HttpMethod.GET, new HttpEntity<>(jsonHeaders()), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(parseData(resp).get("title").asText()).isEqualTo("공개 게시글");
        }

        @Test @Order(4)
        @DisplayName("4. 미인증 - 댓글 목록 조회 가능 (200)")
        void unauthenticatedCanGetComments() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/community/posts/" + postId + "/comments"),
                    HttpMethod.GET, new HttpEntity<>(jsonHeaders()), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(parseData(resp).size()).isEqualTo(1);
        }

        @Test @Order(5)
        @DisplayName("5. 미인증 - 게시글 작성 불가 (403)")
        void unauthenticatedCannotCreatePost() {
            Map<String, Object> body = Map.of("title", "무단 글", "content", "내용");
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/community/posts"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);
            assertThat(resp.getStatusCode().value()).isIn(401, 403);
        }

        @Test @Order(6)
        @DisplayName("6. 미인증 - 댓글 작성 불가 (403)")
        void unauthenticatedCannotCreateComment() {
            Map<String, Object> body = Map.of("content", "무단 댓글");
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/community/posts/" + postId + "/comments"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);
            assertThat(resp.getStatusCode().value()).isIn(401, 403);
        }
    }

    // ══════════════════════════════════════════════════
    // 유효성 검증 테스트
    // ══════════════════════════════════════════════════
    @Nested
    @DisplayName("유효성 검증 테스트")
    class ValidationTests {

        @Test
        @DisplayName("회원가입 - 잘못된 이메일 형식 → 400")
        void signupInvalidEmail() throws Exception {
            Map<String, Object> body = Map.of(
                    "email", "not-an-email", "password", "Password123!", "nickname", "테스터"
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/auth/signup"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(parseRoot(resp).get("success").asBoolean()).isFalse();
        }

        @Test
        @DisplayName("회원가입 - 비밀번호 8자 미만 → 400")
        void signupShortPassword() {
            Map<String, Object> body = Map.of(
                    "email", "short@test.com", "password", "short", "nickname", "테스터"
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/auth/signup"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("회원가입 - 닉네임 1자 → 400")
        void signupShortNickname() {
            Map<String, Object> body = Map.of(
                    "email", "nick@test.com", "password", "Password123!", "nickname", "A"
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/auth/signup"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("분석 - 필수값 누락 → 400")
        void calculateMissingRequired() {
            Map<String, Object> body = Map.of("price", 1000);
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ══════════════════════════════════════════════════
    // 엣지 케이스 테스트
    // ══════════════════════════════════════════════════
    @Nested
    @DisplayName("엣지 케이스 테스트")
    class EdgeCaseTests {

        @Test
        @DisplayName("변동비 0원 시나리오 - 정상 작동 (BEP=17)")
        void calculateWithZeroVariableCost() throws Exception {
            Map<String, Object> body = Map.of(
                    "price", 3000, "variable_cost", 0, "fixed_cost", 50000,
                    "work_hours", 160, "hourly_wage", 9860, "target_profit", 2000000
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("break_even_point").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("17"));
            assertThat(data.get("contribution_margin").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("3000"));
        }

        @Test
        @DisplayName("판매가 = 변동비 → 팔수록 손해 에러 (400)")
        void calculateWithEqualPriceAndVariable() throws Exception {
            Map<String, Object> body = Map.of(
                    "price", 3000, "variable_cost", 3000, "fixed_cost", 50000,
                    "work_hours", 160, "hourly_wage", 9860, "target_profit", 2000000
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            JsonNode root = parseRoot(resp);
            assertThat(root.get("success").asBoolean()).isFalse();
            assertThat(root.get("message").asText()).contains("팔수록 손해");
        }
    }
}
