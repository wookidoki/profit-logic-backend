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
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("전체 기능 통합 테스트 (50개)")
class FullFeatureIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    // ────────────────────────────────────────────────
    // Helper methods (동일 패턴: PersonaE2eIntegrationTest)
    // ────────────────────────────────────────────────

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

    private String signupAndLogin(String email, String password, String nickname) throws Exception {
        Map<String, Object> signupBody = Map.of(
                "email", email, "password", password, "nickname", nickname
        );
        restTemplate.exchange(url("/v1/auth/signup"), HttpMethod.POST,
                new HttpEntity<>(signupBody, jsonHeaders()), String.class);

        Map<String, Object> loginBody = Map.of("email", email, "password", password);
        ResponseEntity<String> loginResp = restTemplate.exchange(url("/v1/auth/login"),
                HttpMethod.POST, new HttpEntity<>(loginBody, jsonHeaders()), String.class);
        return parseData(loginResp).get("access_token").asText();
    }

    // ══════════════════════════════════════════════════════════════
    // Section 1: 10 Virtual New Member Personas (10 tests)
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Section 1 - 가상 신규 회원 페르소나 10인")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class VirtualPersonas {

        // ── Test 1: 핸드메이드 비누 셀러 ──
        @Test @Order(1)
        @DisplayName("P01. 핸드메이드 비누 셀러 - 가입→프로젝트→분석")
        void persona01_handmadeSoap() throws Exception {
            String token = signupAndLogin("handmade01@test.com", "Password1234!", "비누셀러김");

            Map<String, Object> projectBody = Map.of(
                    "title", "핸드메이드 비누 판매",
                    "price", 8000, "variable_cost", 2500, "fixed_cost", 200000,
                    "work_hours", 120, "hourly_wage", 9860, "is_public", false
            );
            ResponseEntity<String> projectResp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            assertThat(projectResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            JsonNode projects = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(projects.size()).isEqualTo(1);

            Map<String, Object> calcBody = Map.of(
                    "price", 8000, "variable_cost", 2500, "fixed_cost", 200000,
                    "work_hours", 120, "hourly_wage", 9860, "target_profit", 2000000
            );
            ResponseEntity<String> calcResp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(calcBody, jsonHeaders()), String.class);
            assertThat(calcResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(calcResp);
            assertThat(data.get("break_even_point").decimalValue()).isPositive();
            assertThat(data.get("contribution_margin").decimalValue()).isPositive();
            assertThat(data.get("is_viable").asBoolean()).isTrue();
        }

        // ── Test 2: 네일아트 프리랜서 ──
        @Test @Order(2)
        @DisplayName("P02. 네일아트 프리랜서 - 가입→프로젝트→분석")
        void persona02_nailArt() throws Exception {
            String token = signupAndLogin("nail01@test.com", "Password1234!", "네일아티스트");

            Map<String, Object> projectBody = Map.of(
                    "title", "네일아트 프리랜서",
                    "price", 40000, "variable_cost", 5000, "fixed_cost", 300000,
                    "work_hours", 160, "hourly_wage", 12000, "is_public", false
            );
            ResponseEntity<String> projectResp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            assertThat(projectResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            JsonNode projects = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(projects.size()).isEqualTo(1);

            Map<String, Object> calcBody = Map.of(
                    "price", 40000, "variable_cost", 5000, "fixed_cost", 300000,
                    "work_hours", 160, "hourly_wage", 12000, "target_profit", 4000000
            );
            ResponseEntity<String> calcResp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(calcBody, jsonHeaders()), String.class);
            assertThat(calcResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(calcResp);
            assertThat(data.get("break_even_point").decimalValue()).isPositive();
            assertThat(data.get("contribution_margin").decimalValue()).isPositive();
            assertThat(data.get("is_viable").asBoolean()).isTrue();
        }

        // ── Test 3: 전자책 작가 ──
        @Test @Order(3)
        @DisplayName("P03. 전자책 작가 - 가입→프로젝트→분석 (변동비 0)")
        void persona03_ebook() throws Exception {
            String token = signupAndLogin("ebook01@test.com", "Password1234!", "전자책작가");

            Map<String, Object> projectBody = Map.of(
                    "title", "전자책 집필 프로젝트",
                    "price", 9900, "variable_cost", 0, "fixed_cost", 100000,
                    "work_hours", 200, "hourly_wage", 9860, "is_public", false
            );
            ResponseEntity<String> projectResp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            assertThat(projectResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            JsonNode projects = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(projects.size()).isEqualTo(1);

            Map<String, Object> calcBody = Map.of(
                    "price", 9900, "variable_cost", 0, "fixed_cost", 100000,
                    "work_hours", 200, "hourly_wage", 9860, "target_profit", 3000000
            );
            ResponseEntity<String> calcResp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(calcBody, jsonHeaders()), String.class);
            assertThat(calcResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(calcResp);
            assertThat(data.get("break_even_point").decimalValue()).isPositive();
            assertThat(data.get("contribution_margin").decimalValue()).isPositive();
            assertThat(data.get("is_viable").asBoolean()).isTrue();
        }

        // ── Test 4: 3D 프린팅 셀러 ──
        @Test @Order(4)
        @DisplayName("P04. 3D 프린팅 셀러 - 가입→프로젝트→분석")
        void persona04_3dPrint() throws Exception {
            String token = signupAndLogin("3dprint01@test.com", "Password1234!", "3D프린터장인");

            Map<String, Object> projectBody = Map.of(
                    "title", "3D 프린팅 굿즈 판매",
                    "price", 25000, "variable_cost", 8000, "fixed_cost", 500000,
                    "work_hours", 80, "hourly_wage", 15000, "is_public", false
            );
            ResponseEntity<String> projectResp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            assertThat(projectResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            JsonNode projects = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(projects.size()).isEqualTo(1);

            Map<String, Object> calcBody = Map.of(
                    "price", 25000, "variable_cost", 8000, "fixed_cost", 500000,
                    "work_hours", 80, "hourly_wage", 15000, "target_profit", 2500000
            );
            ResponseEntity<String> calcResp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(calcBody, jsonHeaders()), String.class);
            assertThat(calcResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(calcResp);
            assertThat(data.get("break_even_point").decimalValue()).isPositive();
            assertThat(data.get("contribution_margin").decimalValue()).isPositive();
            assertThat(data.get("is_viable").asBoolean()).isTrue();
        }

        // ── Test 5: 온라인 과외 ──
        @Test @Order(5)
        @DisplayName("P05. 온라인 과외 - 가입→프로젝트→분석 (변동비 0)")
        void persona05_tutor() throws Exception {
            String token = signupAndLogin("tutor01@test.com", "Password1234!", "수학과외선생");

            Map<String, Object> projectBody = Map.of(
                    "title", "온라인 수학 과외",
                    "price", 50000, "variable_cost", 0, "fixed_cost", 50000,
                    "work_hours", 100, "hourly_wage", 20000, "is_public", false
            );
            ResponseEntity<String> projectResp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            assertThat(projectResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            JsonNode projects = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(projects.size()).isEqualTo(1);

            Map<String, Object> calcBody = Map.of(
                    "price", 50000, "variable_cost", 0, "fixed_cost", 50000,
                    "work_hours", 100, "hourly_wage", 20000, "target_profit", 5000000
            );
            ResponseEntity<String> calcResp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(calcBody, jsonHeaders()), String.class);
            assertThat(calcResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(calcResp);
            assertThat(data.get("break_even_point").decimalValue()).isPositive();
            assertThat(data.get("contribution_margin").decimalValue()).isPositive();
            assertThat(data.get("is_viable").asBoolean()).isTrue();
        }

        // ── Test 6: 수제 쿠키 셀러 ──
        @Test @Order(6)
        @DisplayName("P06. 수제 쿠키 셀러 - 가입→프로젝트→분석")
        void persona06_cookie() throws Exception {
            String token = signupAndLogin("cookie01@test.com", "Password1234!", "쿠키장인이");

            Map<String, Object> projectBody = Map.of(
                    "title", "수제 쿠키 판매",
                    "price", 15000, "variable_cost", 6000, "fixed_cost", 400000,
                    "work_hours", 160, "hourly_wage", 9860, "is_public", false
            );
            ResponseEntity<String> projectResp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            assertThat(projectResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            JsonNode projects = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(projects.size()).isEqualTo(1);

            Map<String, Object> calcBody = Map.of(
                    "price", 15000, "variable_cost", 6000, "fixed_cost", 400000,
                    "work_hours", 160, "hourly_wage", 9860, "target_profit", 3000000
            );
            ResponseEntity<String> calcResp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(calcBody, jsonHeaders()), String.class);
            assertThat(calcResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(calcResp);
            assertThat(data.get("break_even_point").decimalValue()).isPositive();
            assertThat(data.get("contribution_margin").decimalValue()).isPositive();
            assertThat(data.get("is_viable").asBoolean()).isTrue();
        }

        // ── Test 7: 번역 프리랜서 ──
        @Test @Order(7)
        @DisplayName("P07. 번역 프리랜서 - 가입→프로젝트→분석")
        void persona07_translator() throws Exception {
            String token = signupAndLogin("translator01@test.com", "Password1234!", "번역전문가");

            Map<String, Object> projectBody = Map.of(
                    "title", "영한 번역 프리랜서",
                    "price", 30000, "variable_cost", 1000, "fixed_cost", 150000,
                    "work_hours", 180, "hourly_wage", 15000, "is_public", false
            );
            ResponseEntity<String> projectResp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            assertThat(projectResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            JsonNode projects = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(projects.size()).isEqualTo(1);

            Map<String, Object> calcBody = Map.of(
                    "price", 30000, "variable_cost", 1000, "fixed_cost", 150000,
                    "work_hours", 180, "hourly_wage", 15000, "target_profit", 3000000
            );
            ResponseEntity<String> calcResp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(calcBody, jsonHeaders()), String.class);
            assertThat(calcResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(calcResp);
            assertThat(data.get("break_even_point").decimalValue()).isPositive();
            assertThat(data.get("contribution_margin").decimalValue()).isPositive();
            assertThat(data.get("is_viable").asBoolean()).isTrue();
        }

        // ── Test 8: 스티커 디자이너 ──
        @Test @Order(8)
        @DisplayName("P08. 스티커 디자이너 - 가입→프로젝트→분석")
        void persona08_sticker() throws Exception {
            String token = signupAndLogin("sticker01@test.com", "Password1234!", "스티커디자이너");

            Map<String, Object> projectBody = Map.of(
                    "title", "스티커 디자인 판매",
                    "price", 3000, "variable_cost", 500, "fixed_cost", 80000,
                    "work_hours", 60, "hourly_wage", 9860, "is_public", false
            );
            ResponseEntity<String> projectResp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            assertThat(projectResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            JsonNode projects = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(projects.size()).isEqualTo(1);

            Map<String, Object> calcBody = Map.of(
                    "price", 3000, "variable_cost", 500, "fixed_cost", 80000,
                    "work_hours", 60, "hourly_wage", 9860, "target_profit", 1000000
            );
            ResponseEntity<String> calcResp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(calcBody, jsonHeaders()), String.class);
            assertThat(calcResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(calcResp);
            assertThat(data.get("break_even_point").decimalValue()).isPositive();
            assertThat(data.get("contribution_margin").decimalValue()).isPositive();
            assertThat(data.get("is_viable").asBoolean()).isTrue();
        }

        // ── Test 9: 미니어처 공방 ──
        @Test @Order(9)
        @DisplayName("P09. 미니어처 공방 - 가입→프로젝트→분석")
        void persona09_miniature() throws Exception {
            String token = signupAndLogin("miniature01@test.com", "Password1234!", "미니어처장인");

            Map<String, Object> projectBody = Map.of(
                    "title", "미니어처 공방 운영",
                    "price", 45000, "variable_cost", 15000, "fixed_cost", 700000,
                    "work_hours", 200, "hourly_wage", 9860, "is_public", false
            );
            ResponseEntity<String> projectResp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            assertThat(projectResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            JsonNode projects = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(projects.size()).isEqualTo(1);

            Map<String, Object> calcBody = Map.of(
                    "price", 45000, "variable_cost", 15000, "fixed_cost", 700000,
                    "work_hours", 200, "hourly_wage", 9860, "target_profit", 4500000
            );
            ResponseEntity<String> calcResp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(calcBody, jsonHeaders()), String.class);
            assertThat(calcResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(calcResp);
            assertThat(data.get("break_even_point").decimalValue()).isPositive();
            assertThat(data.get("contribution_margin").decimalValue()).isPositive();
            assertThat(data.get("is_viable").asBoolean()).isTrue();
        }

        // ── Test 10: 코딩 부트캠프 ──
        @Test @Order(10)
        @DisplayName("P10. 코딩 부트캠프 - 가입→프로젝트→분석")
        void persona10_bootcamp() throws Exception {
            String token = signupAndLogin("bootcamp01@test.com", "Password1234!", "부트캠프운영자");

            Map<String, Object> projectBody = Map.of(
                    "title", "코딩 부트캠프 운영",
                    "price", 300000, "variable_cost", 20000, "fixed_cost", 2000000,
                    "work_hours", 160, "hourly_wage", 30000, "is_public", false
            );
            ResponseEntity<String> projectResp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            assertThat(projectResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            JsonNode projects = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(projects.size()).isEqualTo(1);

            Map<String, Object> calcBody = Map.of(
                    "price", 300000, "variable_cost", 20000, "fixed_cost", 2000000,
                    "work_hours", 160, "hourly_wage", 30000, "target_profit", 30000000
            );
            ResponseEntity<String> calcResp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(calcBody, jsonHeaders()), String.class);
            assertThat(calcResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(calcResp);
            assertThat(data.get("break_even_point").decimalValue()).isPositive();
            assertThat(data.get("contribution_margin").decimalValue()).isPositive();
            assertThat(data.get("is_viable").asBoolean()).isTrue();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Section 2: Cost Detail CRUD (8 tests)
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Section 2 - 비용 항목 CRUD")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class CostDetailCrud {

        private String token;
        private Long projectId;
        private Long costId1;
        private Long costId2;
        private Long costId3;

        @BeforeAll
        void setup() throws Exception {
            token = signupAndLogin("costuser01@test.com", "Password1234!", "비용테스터");

            Map<String, Object> projectBody = Map.of(
                    "title", "비용 테스트 프로젝트",
                    "price", 20000, "variable_cost", 5000, "fixed_cost", 300000,
                    "work_hours", 100, "hourly_wage", 9860, "is_public", false
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            this.projectId = parseData(resp).get("id").asLong();
        }

        // ── Test 11 ──
        @Test @Order(1)
        @DisplayName("C01. 비용 생성 - MATERIAL/VARIABLE")
        void createCost_material() throws Exception {
            Map<String, Object> body = Map.of(
                    "category", "MATERIAL",
                    "cost_name", "원단 구매비",
                    "cost_type", "VARIABLE",
                    "amount", 15000,
                    "memo", "월간 원단 구매"
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/costs"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            JsonNode data = parseData(resp);
            this.costId1 = data.get("id").asLong();
            assertThat(data.get("cost_name").asText()).isEqualTo("원단 구매비");
            assertThat(data.get("cost_type").asText()).isEqualTo("VARIABLE");
        }

        // ── Test 12 ──
        @Test @Order(2)
        @DisplayName("C02. 비용 생성 - TOOL_SUBSCRIPTION/FIXED")
        void createCost_tool() throws Exception {
            Map<String, Object> body = Map.of(
                    "category", "TOOL_SUBSCRIPTION",
                    "cost_name", "Adobe 구독료",
                    "cost_type", "FIXED",
                    "amount", 24000,
                    "memo", "월 구독"
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/costs"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            JsonNode data = parseData(resp);
            this.costId2 = data.get("id").asLong();
            assertThat(data.get("category").asText()).isEqualTo("TOOL_SUBSCRIPTION");
        }

        // ── Test 13 ──
        @Test @Order(3)
        @DisplayName("C03. 비용 생성 - MARKETING/FIXED")
        void createCost_marketing() throws Exception {
            Map<String, Object> body = Map.of(
                    "category", "MARKETING",
                    "cost_name", "인스타그램 광고비",
                    "cost_type", "FIXED",
                    "amount", 100000,
                    "memo", "월 광고 예산"
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/costs"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            JsonNode data = parseData(resp);
            this.costId3 = data.get("id").asLong();
            assertThat(data.get("category").asText()).isEqualTo("MARKETING");
        }

        // ── Test 14 ──
        @Test @Order(4)
        @DisplayName("C04. 비용 목록 조회 → 3건")
        void listCosts_3() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/costs"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.isArray()).isTrue();
            assertThat(data.size()).isEqualTo(3);
        }

        // ── Test 15 ──
        @Test @Order(5)
        @DisplayName("C05. 비용 1건 삭제")
        void deleteCost() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/costs/" + costId3),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        // ── Test 16 ──
        @Test @Order(6)
        @DisplayName("C06. 비용 목록 조회 → 2건")
        void listCosts_2() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/costs"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.size()).isEqualTo(2);
        }

        // ── Test 17 ──
        @Test @Order(7)
        @DisplayName("C07. 비용 금액 0원 등록 → 400 (0보다 커야 함)")
        void createCost_zeroAmount() throws Exception {
            Map<String, Object> body = Map.of(
                    "category", "OTHER",
                    "cost_name", "무료 항목",
                    "cost_type", "FIXED",
                    "amount", 0
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/costs"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        // ── Test 18 ──
        @Test @Order(8)
        @DisplayName("C08. 프로젝트 분석에 비용 반영 확인")
        void projectAnalysis_reflectsCosts() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/analysis"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("project_id").asLong()).isEqualTo(projectId);
            assertThat(data.has("cost_breakdown")).isTrue();
            assertThat(data.has("bep")).isTrue();
            assertThat(data.has("shadow_wage")).isTrue();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Section 3: Time Log CRUD (7 tests)
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Section 3 - 시간 기록 CRUD")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TimeLogCrud {

        private String token;
        private Long projectId;
        private Long timeLogId1;
        private Long timeLogId2;
        private Long timeLogId3;

        @BeforeAll
        void setup() throws Exception {
            token = signupAndLogin("timeloguser01@test.com", "Password1234!", "시간기록테스터");

            Map<String, Object> projectBody = Map.of(
                    "title", "시간 기록 테스트 프로젝트",
                    "price", 30000, "variable_cost", 5000, "fixed_cost", 200000,
                    "work_hours", 120, "hourly_wage", 9860, "is_public", false
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            this.projectId = parseData(resp).get("id").asLong();
        }

        // ── Test 19 ──
        @Test @Order(1)
        @DisplayName("T01. 시간 기록 생성 - 오늘, 3시간")
        void createTimeLog_today() throws Exception {
            Map<String, Object> body = Map.of(
                    "task_name", "디자인 작업",
                    "hours_spent", 3.0,
                    "log_date", LocalDate.now().toString(),
                    "memo", "메인 디자인"
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/timelogs"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            JsonNode data = parseData(resp);
            this.timeLogId1 = data.get("id").asLong();
            assertThat(data.get("task_name").asText()).isEqualTo("디자인 작업");
            assertThat(data.get("hours_spent").decimalValue()).isEqualByComparingTo(new BigDecimal("3.0"));
        }

        // ── Test 20 ──
        @Test @Order(2)
        @DisplayName("T02. 시간 기록 생성 - 어제, 5시간")
        void createTimeLog_yesterday() throws Exception {
            Map<String, Object> body = Map.of(
                    "task_name", "코딩 작업",
                    "hours_spent", 5.0,
                    "log_date", LocalDate.now().minusDays(1).toString(),
                    "memo", "백엔드 구현"
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/timelogs"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            JsonNode data = parseData(resp);
            this.timeLogId2 = data.get("id").asLong();
            assertThat(data.get("hours_spent").decimalValue()).isEqualByComparingTo(new BigDecimal("5.0"));
        }

        // ── Test 21 ──
        @Test @Order(3)
        @DisplayName("T03. 시간 기록 생성 - 7일 전, 2시간")
        void createTimeLog_weekAgo() throws Exception {
            Map<String, Object> body = Map.of(
                    "task_name", "기획 회의",
                    "hours_spent", 2.0,
                    "log_date", LocalDate.now().minusDays(7).toString(),
                    "memo", "주간 기획 회의"
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/timelogs"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            JsonNode data = parseData(resp);
            this.timeLogId3 = data.get("id").asLong();
            assertThat(data.get("hours_spent").decimalValue()).isEqualByComparingTo(new BigDecimal("2.0"));
        }

        // ── Test 22 ──
        @Test @Order(4)
        @DisplayName("T04. 시간 기록 전체 조회 → 3건")
        void listTimeLogs() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/timelogs"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.isArray()).isTrue();
            assertThat(data.size()).isEqualTo(3);
        }

        // ── Test 23 ──
        @Test @Order(5)
        @DisplayName("T05. 총 작업 시간 조회 → 10.0시간")
        void getTotalHours() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/timelogs/total"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.decimalValue()).isEqualByComparingTo(new BigDecimal("10.0"));
        }

        // ── Test 24 ──
        @Test @Order(6)
        @DisplayName("T06. 시간 기록 1건 삭제")
        void deleteTimeLog() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/timelogs/" + timeLogId3),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        // ── Test 25 ──
        @Test @Order(7)
        @DisplayName("T07. 삭제 후 목록 조회 → 2건")
        void listTimeLogs_afterDelete() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/timelogs"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.size()).isEqualTo(2);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Section 4: Dashboard Summary (5 tests)
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Section 4 - 대시보드 요약")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class DashboardSummary {

        private String token;
        private Long projectId1;
        private Long projectId2;

        @BeforeAll
        void setup() throws Exception {
            token = signupAndLogin("dashboard01@test.com", "Password1234!", "대시보드테스터");
        }

        // ── Test 26 ──
        @Test @Order(1)
        @DisplayName("D01. 2개 프로젝트 생성")
        void createTwoProjects() throws Exception {
            Map<String, Object> body1 = Map.of(
                    "title", "대시보드 프로젝트 A",
                    "price", 20000, "variable_cost", 5000, "fixed_cost", 300000,
                    "work_hours", 120, "hourly_wage", 9860, "is_public", false
            );
            ResponseEntity<String> resp1 = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(body1, authHeaders(token)), String.class);
            assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            this.projectId1 = parseData(resp1).get("id").asLong();

            Map<String, Object> body2 = Map.of(
                    "title", "대시보드 프로젝트 B",
                    "price", 50000, "variable_cost", 10000, "fixed_cost", 500000,
                    "work_hours", 160, "hourly_wage", 15000, "is_public", false
            );
            ResponseEntity<String> resp2 = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(body2, authHeaders(token)), String.class);
            assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            this.projectId2 = parseData(resp2).get("id").asLong();
        }

        // ── Test 27 ──
        @Test @Order(2)
        @DisplayName("D02. 대시보드 요약 조회 → totalProjects = 2")
        void dashboardSummary_totalProjects() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/dashboard/summary"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("total_projects").asInt()).isEqualTo(2);
        }

        // ── Test 28 ──
        @Test @Order(3)
        @DisplayName("D03. avgShadowWage > 0 확인")
        void dashboardSummary_avgShadowWage() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/dashboard/summary"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            JsonNode data = parseData(resp);
            assertThat(data.get("avg_shadow_wage").decimalValue()).isPositive();
        }

        // ── Test 29 ──
        @Test @Order(4)
        @DisplayName("D04. avgContributionMarginRate > 0 확인")
        void dashboardSummary_avgMarginRate() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/dashboard/summary"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            JsonNode data = parseData(resp);
            assertThat(data.get("avg_contribution_margin_rate").decimalValue()).isPositive();
        }

        // ── Test 30 ──
        @Test @Order(5)
        @DisplayName("D05. projects 배열 2건 + status 필드 존재")
        void dashboardSummary_projectsArray() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/dashboard/summary"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            JsonNode data = parseData(resp);
            JsonNode projects = data.get("projects");
            assertThat(projects.isArray()).isTrue();
            assertThat(projects.size()).isEqualTo(2);
            assertThat(projects.get(0).has("status")).isTrue();
            assertThat(projects.get(1).has("status")).isTrue();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Section 5: Goal Tracking (6 tests)
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Section 5 - 목표 추적")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class GoalTracking {

        private String token;
        private Long projectId;

        @BeforeAll
        void setup() throws Exception {
            token = signupAndLogin("goaluser01@test.com", "Password1234!", "목표추적테스터");

            Map<String, Object> projectBody = Map.of(
                    "title", "목표 추적 테스트 프로젝트",
                    "price", 25000, "variable_cost", 5000, "fixed_cost", 300000,
                    "work_hours", 120, "hourly_wage", 9860, "is_public", false
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            this.projectId = parseData(resp).get("id").asLong();
        }

        // ── Test 31 ──
        @Test @Order(1)
        @DisplayName("G01. 목표 미설정 → status = NO_TARGET")
        void goal_noTarget() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/goal"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.get("status").asText()).isEqualTo("NO_TARGET");
        }

        // ── Test 32 ──
        @Test @Order(2)
        @DisplayName("G02. 목표 설정 - targetRevenue=500000, targetMonth=2026-12")
        void goal_set() throws Exception {
            Map<String, Object> body = Map.of(
                    "target_revenue", 500000,
                    "target_month", "2026-12"
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/goal"),
                    HttpMethod.PUT, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        // ── Test 33 ──
        @Test @Order(3)
        @DisplayName("G03. 목표 조회 → status != NO_TARGET")
        void goal_statusNotNoTarget() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/goal"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            String status = data.get("status").asText();
            assertThat(status).isIn("ON_TRACK", "BEHIND", "URGENT");
        }

        // ── Test 34 ──
        @Test @Order(4)
        @DisplayName("G04. monthsRemaining > 0 확인")
        void goal_monthsRemaining() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/goal"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            JsonNode data = parseData(resp);
            assertThat(data.get("months_remaining").asInt()).isPositive();
        }

        // ── Test 35 ──
        @Test @Order(5)
        @DisplayName("G05. bepQuantity > 0 확인")
        void goal_bepQuantity() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/goal"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            JsonNode data = parseData(resp);
            assertThat(data.get("bep_quantity").decimalValue()).isPositive();
        }

        // ── Test 36 ──
        @Test @Order(6)
        @DisplayName("G06. 목표 해제 → status = NO_TARGET")
        void goal_clear() throws Exception {
            String bodyJson = "{\"target_revenue\": null, \"target_month\": null}";
            HttpHeaders headers = authHeaders(token);
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/goal"),
                    HttpMethod.PUT, new HttpEntity<>(bodyJson, headers), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

            ResponseEntity<String> getResp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/goal"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);
            JsonNode data = parseData(getResp);
            assertThat(data.get("status").asText()).isEqualTo("NO_TARGET");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Section 6: Monthly Trends (4 tests)
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Section 6 - 월별 트렌드")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class MonthlyTrends {

        private String token;
        private Long projectId;

        @BeforeAll
        void setup() throws Exception {
            token = signupAndLogin("trenduser01@test.com", "Password1234!", "트렌드테스터");

            Map<String, Object> projectBody = Map.of(
                    "title", "트렌드 테스트 프로젝트",
                    "price", 20000, "variable_cost", 4000, "fixed_cost", 250000,
                    "work_hours", 100, "hourly_wage", 9860, "is_public", false
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(projectBody, authHeaders(token)), String.class);
            this.projectId = parseData(resp).get("id").asLong();
        }

        // ── Test 37 ──
        @Test @Order(1)
        @DisplayName("M01. 비용/시간 기록 추가 (현재 월)")
        void addCostsAndTimelogs() throws Exception {
            // 비용 추가
            Map<String, Object> costBody = Map.of(
                    "category", "MATERIAL",
                    "cost_name", "재료비",
                    "cost_type", "VARIABLE",
                    "amount", 50000
            );
            ResponseEntity<String> costResp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/costs"),
                    HttpMethod.POST, new HttpEntity<>(costBody, authHeaders(token)), String.class);
            assertThat(costResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            // 시간 기록 추가
            Map<String, Object> timeBody = Map.of(
                    "task_name", "제품 제작",
                    "hours_spent", 4.0,
                    "log_date", LocalDate.now().toString()
            );
            ResponseEntity<String> timeResp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/timelogs"),
                    HttpMethod.POST, new HttpEntity<>(timeBody, authHeaders(token)), String.class);
            assertThat(timeResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        // ── Test 38 ──
        @Test @Order(2)
        @DisplayName("M02. 월별 트렌드 조회 성공 (months=6)")
        void getTrends_success() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/trends?months=6"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        // ── Test 39 ──
        @Test @Order(3)
        @DisplayName("M03. snapshots 배열 확인")
        void getTrends_snapshotsArray() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/trends?months=6"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            JsonNode data = parseData(resp);
            assertThat(data.has("snapshots")).isTrue();
            assertThat(data.get("snapshots").isArray()).isTrue();
        }

        // ── Test 40 ──
        @Test @Order(4)
        @DisplayName("M04. snapshots 항목 필드 확인 (month, total_cost, shadow_wage)")
        void getTrends_snapshotFields() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/trends?months=6"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            JsonNode data = parseData(resp);
            JsonNode snapshots = data.get("snapshots");
            if (snapshots.size() > 0) {
                JsonNode first = snapshots.get(0);
                assertThat(first.has("month")).isTrue();
                assertThat(first.has("total_cost")).isTrue();
                assertThat(first.has("shadow_wage")).isTrue();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Section 7: Project Analysis & Price Simulation (5 tests)
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Section 7 - 프로젝트 분석 & 가격 시뮬레이션")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ProjectAnalysisAndPriceSimulation {

        private String token;
        private Long projectId;

        @BeforeAll
        void setup() throws Exception {
            token = signupAndLogin("analysisuser01@test.com", "Password1234!", "분석시뮬테스터");
        }

        // ── Test 41 ──
        @Test @Order(1)
        @DisplayName("A01. 프로젝트 생성")
        void createProject() throws Exception {
            Map<String, Object> body = Map.of(
                    "title", "가격 시뮬레이션 테스트",
                    "price", 50000, "variable_cost", 10000, "fixed_cost", 500000,
                    "work_hours", 150, "hourly_wage", 15000, "is_public", false
            );
            ResponseEntity<String> resp = restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            this.projectId = parseData(resp).get("id").asLong();
        }

        // ── Test 42 ──
        @Test @Order(2)
        @DisplayName("A02. 프로젝트 분석 → bep, shadow_wage, contribution_margin 존재")
        void projectAnalysis() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/analysis"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.has("bep")).isTrue();
            assertThat(data.has("shadow_wage")).isTrue();
            assertThat(data.has("cost_breakdown")).isTrue();
        }

        // ── Test 43 ──
        @Test @Order(3)
        @DisplayName("A03. 가격 시뮬레이션 (newPrice=70000) → 응답 확인")
        void priceSimulation() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId + "/analysis/price-simulation?newPrice=70000"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.has("current_price")).isTrue();
            assertThat(data.has("new_price")).isTrue();
            assertThat(data.get("new_price").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("70000"));
        }

        // ── Test 44 ──
        @Test @Order(4)
        @DisplayName("A04. 가격 인상 시 BEP 감소 원리 확인")
        void priceIncrease_bepDecrease() throws Exception {
            // 원래 가격 50000으로 분석
            Map<String, Object> originalCalc = Map.of(
                    "price", 50000, "variable_cost", 10000, "fixed_cost", 500000,
                    "work_hours", 150, "hourly_wage", 15000, "target_profit", 5000000
            );
            ResponseEntity<String> origResp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(originalCalc, jsonHeaders()), String.class);
            BigDecimal originalBep = parseData(origResp).get("break_even_point").decimalValue();

            // 인상 가격 70000으로 분석
            Map<String, Object> increasedCalc = Map.of(
                    "price", 70000, "variable_cost", 10000, "fixed_cost", 500000,
                    "work_hours", 150, "hourly_wage", 15000, "target_profit", 5000000
            );
            ResponseEntity<String> newResp = restTemplate.exchange(url("/v1/analysis/calculate"),
                    HttpMethod.POST, new HttpEntity<>(increasedCalc, jsonHeaders()), String.class);
            BigDecimal newBep = parseData(newResp).get("break_even_point").decimalValue();

            assertThat(newBep).isLessThan(originalBep);
        }

        // ── Test 45 ──
        @Test @Order(5)
        @DisplayName("A05. 프로젝트 삭제 정리")
        void cleanup() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/projects/" + projectId),
                    HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode data = parseData(restTemplate.exchange(url("/v1/projects"),
                    HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class));
            assertThat(data.size()).isEqualTo(0);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Section 8: Script Analysis (5 tests)
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Section 8 - 스크립트 분석")
    class ScriptAnalysis {

        // ── Test 46 ──
        @Test
        @DisplayName("S01. 카테고리 목록 조회 → 5개 항목")
        void getCategories() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/scripts/categories"),
                    HttpMethod.GET, new HttpEntity<>(jsonHeaders()), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.isArray()).isTrue();
            assertThat(data.size()).isEqualTo(5);
        }

        // ── Test 47 ──
        @Test
        @DisplayName("S02. WEB_NOVEL 템플릿 조회 → 필드 존재")
        void getTemplate_webNovel() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/scripts/templates/WEB_NOVEL"),
                    HttpMethod.GET, new HttpEntity<>(jsonHeaders()), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.has("category")).isTrue();
            assertThat(data.has("display_name")).isTrue();
            assertThat(data.has("sections")).isTrue();
        }

        // ── Test 48 ──
        @Test
        @DisplayName("S03. EMOTICON 템플릿 조회 → 필드 존재")
        void getTemplate_emoticon() throws Exception {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/scripts/templates/EMOTICON"),
                    HttpMethod.GET, new HttpEntity<>(jsonHeaders()), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.has("category")).isTrue();
            assertThat(data.has("display_name")).isTrue();
            assertThat(data.has("sections")).isTrue();
        }

        // ── Test 49 ──
        @Test
        @DisplayName("S04. WEB_NOVEL 스크립트 분석 → 성공")
        void analyze_webNovel() throws Exception {
            Map<String, Object> inputs = Map.of(
                    "episodePrice", "300",
                    "revenueShareRate", "70",
                    "episodesPerMonth", "30",
                    "writingHoursPerEpisode", "3",
                    "monthlyFixedCost", "50000",
                    "hourlyWage", "9860",
                    "targetMonthlyIncome", "2000000"
            );
            Map<String, Object> body = Map.of(
                    "category", "WEB_NOVEL",
                    "inputs", inputs
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/scripts/analyze"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.has("category")).isTrue();
            assertThat(data.get("category").asText()).isEqualTo("WEB_NOVEL");
            assertThat(data.has("result")).isTrue();
            assertThat(data.get("result").has("break_even_point")).isTrue();
        }

        // ── Test 50 ──
        @Test
        @DisplayName("S05. INDIE_DEV 스크립트 분석 → 성공")
        void analyze_indieDev() throws Exception {
            Map<String, Object> inputs = Map.of(
                    "unitPrice", "15000",
                    "platformFeeRate", "30",
                    "serverMonthlyCost", "50000",
                    "toolsMonthlyCost", "20000",
                    "developmentHoursPerMonth", "120",
                    "supportHoursPerMonth", "20",
                    "marketingHoursPerMonth", "20",
                    "hourlyWage", "9860",
                    "targetMonthlyIncome", "3000000"
            );
            Map<String, Object> body = Map.of(
                    "category", "INDIE_DEV",
                    "inputs", inputs
            );
            ResponseEntity<String> resp = restTemplate.exchange(
                    url("/v1/scripts/analyze"),
                    HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = parseData(resp);
            assertThat(data.has("category")).isTrue();
            assertThat(data.get("category").asText()).isEqualTo("INDIE_DEV");
            assertThat(data.has("result")).isTrue();
            assertThat(data.get("result").has("break_even_point")).isTrue();
            assertThat(data.get("result").has("is_viable")).isTrue();
        }
    }
}
