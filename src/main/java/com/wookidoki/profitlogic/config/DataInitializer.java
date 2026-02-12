package com.wookidoki.profitlogic.config;

import com.wookidoki.profitlogic.domain.*;
import com.wookidoki.profitlogic.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"local", "prod"})
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final CostDetailRepository costDetailRepository;
    private final TimeLogRepository timeLogRepository;
    private final SimulationRepository simulationRepository;
    private final BoardPostRepository boardPostRepository;
    private final CommentRepository commentRepository;
    private final ChatLogRepository chatLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail("admin@profitlogic.com")) {
            log.info("[DataInitializer] 시드 데이터 이미 존재 → 스킵");
            return;
        }

        log.info("[DataInitializer] 시드 데이터 생성 시작...");

        String encodedPw = passwordEncoder.encode("Test1234!");

        // ══════════════════════════════════════════
        // 관리자 계정
        // ══════════════════════════════════════════
        User admin = userRepository.save(User.builder()
                .email("admin@profitlogic.com")
                .password(encodedPw)
                .nickname("관리자")
                .role(Role.ROLE_ADMIN)
                .build());

        projectRepository.save(Project.builder()
                .user(admin)
                .title("관리자 테스트 프로젝트")
                .price(bd("10000"))
                .variableCost(bd("3000"))
                .fixedCost(bd("200000"))
                .workHours(160)
                .hourlyWage(bd("9860"))
                .isPublic(false)
                .build());

        // ══════════════════════════════════════════
        // 페르소나 1~10: 크리에이터 (CREATOR)
        // ══════════════════════════════════════════

        // 1. 웹소설 작가 (고시간, 저비용, 회차당 저가)
        User u1 = createUser("creator01@test.com", encodedPw, "웹소설작가김", BizType.CREATOR);
        Project p1 = createProject(u1, "웹소설 연재 수익 분석", "3000", "100", "50000", 160, "9860", true);
        addCost(p1, CostCategory.TOOL_SUBSCRIPTION, "노트북 감가상각", "FIXED", "30000", null);
        addCost(p1, CostCategory.OTHER, "자료구매비", "VARIABLE", "5000", "월평균 참고서적");
        addTimeLog(p1, "원고 집필 (1~3화)", "8.0", -1, null);
        addTimeLog(p1, "퇴고 및 교정", "3.0", -2, null);
        addTimeLog(p1, "세계관 설정", "5.0", -5, "초기 설정 작업");

        // 2. 이모티콘 작가 (고정비 낮음, 변동비 거의 0)
        User u2 = createUser("creator02@test.com", encodedPw, "이모티콘작가이", BizType.CREATOR);
        Project p2 = createProject(u2, "카카오 이모티콘 세트", "2000", "0", "20000", 120, "9860", true);
        addCost(p2, CostCategory.TOOL_SUBSCRIPTION, "프로크리에이트 구독", "FIXED", "12000", null);
        addTimeLog(p2, "캐릭터 디자인", "6.0", -1, "기본형 24종");
        addTimeLog(p2, "움직이는 이모티콘", "8.0", -2, "애니메이션 작업");

        // 3. 유튜브 크리에이터 (고정비 높음, 장비투자)
        User u3 = createUser("creator03@test.com", encodedPw, "유튜버박", BizType.CREATOR);
        Project p3 = createProject(u3, "유튜브 채널 운영", "50000", "5000", "800000", 200, "9860", true);
        addCost(p3, CostCategory.SERVER, "클라우드 스토리지", "FIXED", "15000", "구글원 2TB");
        addCost(p3, CostCategory.TOOL_SUBSCRIPTION, "편집 소프트웨어", "FIXED", "25000", "프리미어 프로");
        addCost(p3, CostCategory.MARKETING, "썸네일 외주", "VARIABLE", "30000", "영상당 단가");
        addCost(p3, CostCategory.OTHER, "장비 감가상각", "FIXED", "200000", "카메라+조명+마이크");
        addTimeLog(p3, "영상 촬영", "4.0", -1, null);
        addTimeLog(p3, "영상 편집", "8.0", -1, "컷편집+자막+효과음");
        addTimeLog(p3, "기획 회의", "2.0", -3, null);

        // 4. 블로그 운영자 (광고수익, 저비용)
        User u4 = createUser("creator04@test.com", encodedPw, "블로거최", BizType.CREATOR);
        Project p4 = createProject(u4, "네이버 블로그 광고 수익", "500", "0", "30000", 80, "9860", false);
        addCost(p4, CostCategory.TOOL_SUBSCRIPTION, "유료 이미지 소스", "FIXED", "10000", null);
        addTimeLog(p4, "포스팅 작성", "2.0", -1, "일일 1포스팅");

        // 5. 웹툰 작가 (고시간, 중비용)
        User u5 = createUser("creator05@test.com", encodedPw, "웹툰작가정", BizType.CREATOR);
        Project p5 = createProject(u5, "웹툰 연재 프로젝트", "5000", "200", "300000", 240, "9860", true);
        addCost(p5, CostCategory.TOOL_SUBSCRIPTION, "클립스튜디오 라이선스", "FIXED", "8000", null);
        addCost(p5, CostCategory.OUTSOURCING, "배경 외주", "VARIABLE", "150000", "회당 배경 5컷");
        addTimeLog(p5, "콘티 작업", "4.0", -1, null);
        addTimeLog(p5, "펜선 + 채색", "12.0", -1, "주 2회 연재분");

        // 6. 팟캐스트 진행자
        User u6 = createUser("creator06@test.com", encodedPw, "팟캐스터강", BizType.CREATOR);
        Project p6 = createProject(u6, "팟캐스트 채널 운영", "30000", "2000", "100000", 60, "9860", false);
        addCost(p6, CostCategory.SERVER, "팟캐스트 호스팅", "FIXED", "15000", null);
        addCost(p6, CostCategory.OTHER, "마이크 장비 감가상각", "FIXED", "50000", null);
        addTimeLog(p6, "에피소드 녹음", "2.0", -1, null);
        addTimeLog(p6, "편집 및 업로드", "3.0", -2, null);

        // 7. 숏폼 크리에이터 (틱톡/릴스)
        User u7 = createUser("creator07@test.com", encodedPw, "숏폼크리에이터윤", BizType.CREATOR);
        Project p7 = createProject(u7, "틱톡 숏폼 채널", "20000", "1000", "150000", 100, "9860", true);
        addCost(p7, CostCategory.MARKETING, "인플루언서 콜라보", "VARIABLE", "50000", null);
        addTimeLog(p7, "촬영 + 편집", "3.0", -1, "일일 1~2편 제작");

        // 8. 온라인 강의 제작자
        User u8 = createUser("creator08@test.com", encodedPw, "강의크리에이터한", BizType.CREATOR);
        Project p8 = createProject(u8, "프로그래밍 온라인 강의", "35000", "0", "500000", 180, "15000", true);
        addCost(p8, CostCategory.TOOL_SUBSCRIPTION, "화면녹화 소프트웨어", "FIXED", "15000", null);
        addCost(p8, CostCategory.SERVER, "강의 호스팅 플랫폼", "FIXED", "50000", null);
        addTimeLog(p8, "강의 커리큘럼 설계", "6.0", -7, null);
        addTimeLog(p8, "강의 녹화", "4.0", -1, null);
        addTimeLog(p8, "편집 및 자막", "5.0", -2, null);

        // 9. 디지털 일러스트 작가
        User u9 = createUser("creator09@test.com", encodedPw, "일러스트레이터서", BizType.CREATOR);
        Project p9 = createProject(u9, "커미션 일러스트", "80000", "0", "60000", 100, "9860", false);
        addCost(p9, CostCategory.TOOL_SUBSCRIPTION, "포토샵+와콤 태블릿", "FIXED", "40000", null);
        addTimeLog(p9, "의뢰 작업 (러프~완성)", "6.0", -1, "커미션 1건");

        // 10. 음악 프로듀서
        User u10 = createUser("creator10@test.com", encodedPw, "프로듀서오", BizType.CREATOR);
        Project p10 = createProject(u10, "비트 판매 프로젝트", "15000", "0", "200000", 120, "9860", true);
        addCost(p10, CostCategory.TOOL_SUBSCRIPTION, "DAW 라이선스", "FIXED", "25000", "FL Studio");
        addCost(p10, CostCategory.TOOL_SUBSCRIPTION, "음원 샘플팩", "FIXED", "30000", null);
        addTimeLog(p10, "비트메이킹", "4.0", -1, null);
        addTimeLog(p10, "믹싱+마스터링", "3.0", -2, null);

        // ══════════════════════════════════════════
        // 페르소나 11~20: 셀러 (SELLER)
        // ══════════════════════════════════════════

        // 11. 핸드메이드 양초 셀러
        User u11 = createUser("seller01@test.com", encodedPw, "양초셀러김", BizType.SELLER);
        Project p11 = createProject(u11, "핸드메이드 소이캔들", "15000", "5000", "500000", 160, "9860", true);
        addCost(p11, CostCategory.MATERIAL, "소이왁스+향료", "VARIABLE", "3000", "캔들 1개 기준");
        addCost(p11, CostCategory.MATERIAL, "용기+패키지", "VARIABLE", "2000", null);
        addCost(p11, CostCategory.MARKETING, "스마트스토어 광고", "FIXED", "200000", null);
        addCost(p11, CostCategory.OTHER, "작업실 임대료", "FIXED", "300000", null);
        addTimeLog(p11, "양초 제작 (10개)", "5.0", -1, null);
        addTimeLog(p11, "포장 및 발송", "2.0", -1, null);

        // 12. 스마트스토어 의류 셀러
        User u12 = createUser("seller02@test.com", encodedPw, "의류셀러박", BizType.SELLER);
        Project p12 = createProject(u12, "여성의류 스마트스토어", "35000", "18000", "2000000", 200, "9860", true);
        addCost(p12, CostCategory.MATERIAL, "의류 사입비", "VARIABLE", "15000", "개당 평균");
        addCost(p12, CostCategory.OUTSOURCING, "사진촬영 외주", "VARIABLE", "3000", null);
        addCost(p12, CostCategory.MARKETING, "네이버 쇼핑 광고", "FIXED", "500000", null);
        addCost(p12, CostCategory.OTHER, "물류 창고", "FIXED", "800000", null);
        addTimeLog(p12, "상품 소싱", "6.0", -3, "동대문 방문");
        addTimeLog(p12, "상품 등록", "3.0", -1, "5개 신상품");

        // 13. 쿠팡 가전 셀러
        User u13 = createUser("seller03@test.com", encodedPw, "가전셀러이", BizType.SELLER);
        Project p13 = createProject(u13, "쿠팡 소형가전 판매", "45000", "30000", "1500000", 160, "9860", false);
        addCost(p13, CostCategory.MATERIAL, "제품 매입비", "VARIABLE", "28000", null);
        addCost(p13, CostCategory.OUTSOURCING, "배송 대행", "VARIABLE", "2000", null);
        addCost(p13, CostCategory.MARKETING, "쿠팡 광고비", "FIXED", "800000", null);
        addTimeLog(p13, "재고 관리 + 발주", "3.0", -1, null);

        // 14. 에어팟 액세서리 셀러
        User u14 = createUser("seller04@test.com", encodedPw, "액세서리셀러최", BizType.SELLER);
        Project p14 = createProject(u14, "에어팟 케이스 판매", "12000", "3500", "300000", 100, "9860", true);
        addCost(p14, CostCategory.MATERIAL, "케이스 제조 원가", "VARIABLE", "2500", "알리 사입");
        addCost(p14, CostCategory.MARKETING, "인스타 광고", "FIXED", "150000", null);
        addTimeLog(p14, "상품 촬영 + 등록", "4.0", -2, null);
        addTimeLog(p14, "CS 응대", "1.0", -1, null);

        // 15. 수제 디저트 셀러
        User u15 = createUser("seller05@test.com", encodedPw, "디저트셀러정", BizType.SELLER);
        Project p15 = createProject(u15, "수제 마카롱 판매", "25000", "10000", "800000", 180, "9860", true);
        addCost(p15, CostCategory.MATERIAL, "재료비 (12개 세트)", "VARIABLE", "8000", "버터+아몬드가루");
        addCost(p15, CostCategory.MATERIAL, "포장재", "VARIABLE", "2000", null);
        addCost(p15, CostCategory.OTHER, "공유주방 임대", "FIXED", "500000", null);
        addCost(p15, CostCategory.MARKETING, "배달앱 수수료", "FIXED", "150000", "월 고정");
        addTimeLog(p15, "마카롱 제작", "6.0", -1, "50개 1배치");
        addTimeLog(p15, "포장+배송 준비", "2.0", -1, null);

        // 16. 해외직구 셀러
        User u16 = createUser("seller06@test.com", encodedPw, "직구셀러강", BizType.SELLER);
        Project p16 = createProject(u16, "해외직구 건강식품", "28000", "15000", "1000000", 140, "9860", false);
        addCost(p16, CostCategory.MATERIAL, "해외 매입비", "VARIABLE", "12000", "관세 포함");
        addCost(p16, CostCategory.OUTSOURCING, "통관 대행", "VARIABLE", "3000", null);
        addCost(p16, CostCategory.MARKETING, "블로그 체험단", "FIXED", "400000", null);
        addTimeLog(p16, "상품 리서치", "3.0", -3, null);
        addTimeLog(p16, "발주+통관 관리", "2.0", -1, null);

        // 17. 반려동물 용품 셀러
        User u17 = createUser("seller07@test.com", encodedPw, "펫용품셀러윤", BizType.SELLER);
        Project p17 = createProject(u17, "수제 강아지 간식", "18000", "7000", "600000", 160, "9860", true);
        addCost(p17, CostCategory.MATERIAL, "닭가슴살+고구마", "VARIABLE", "5000", null);
        addCost(p17, CostCategory.MATERIAL, "포장+라벨", "VARIABLE", "2000", null);
        addCost(p17, CostCategory.OTHER, "식약처 인증비", "FIXED", "100000", "월할 환산");
        addTimeLog(p17, "간식 제조", "5.0", -1, "건조기 가동 포함");

        // 18. 문구류 셀러
        User u18 = createUser("seller08@test.com", encodedPw, "문구셀러한", BizType.SELLER);
        Project p18 = createProject(u18, "감성 다이어리 판매", "22000", "8000", "400000", 100, "9860", true);
        addCost(p18, CostCategory.MATERIAL, "인쇄비", "VARIABLE", "6000", "100부 기준 단가");
        addCost(p18, CostCategory.MATERIAL, "바인딩+부자재", "VARIABLE", "2000", null);
        addCost(p18, CostCategory.MARKETING, "인스타+블로그", "FIXED", "200000", null);
        addTimeLog(p18, "디자인 작업", "8.0", -5, "월간 신상품");

        // 19. 화장품 셀러 (마진 높음)
        User u19 = createUser("seller09@test.com", encodedPw, "화장품셀러서", BizType.SELLER);
        Project p19 = createProject(u19, "자체 브랜드 스킨케어", "32000", "8000", "3000000", 200, "12000", false);
        addCost(p19, CostCategory.OUTSOURCING, "OEM 제조비", "VARIABLE", "6000", null);
        addCost(p19, CostCategory.MATERIAL, "용기+패키지", "VARIABLE", "2000", null);
        addCost(p19, CostCategory.MARKETING, "인플루언서 마케팅", "FIXED", "1500000", null);
        addCost(p19, CostCategory.OTHER, "사무실 임대", "FIXED", "500000", null);
        addTimeLog(p19, "마케팅 전략 수립", "4.0", -3, null);
        addTimeLog(p19, "CS + 리뷰 관리", "2.0", -1, null);

        // 20. 중고거래 리셀러
        User u20 = createUser("seller10@test.com", encodedPw, "리셀러오", BizType.SELLER);
        Project p20 = createProject(u20, "한정판 스니커즈 리셀", "350000", "250000", "100000", 40, "9860", false);
        addCost(p20, CostCategory.MATERIAL, "스니커즈 매입가", "VARIABLE", "250000", "시즌 평균");
        addTimeLog(p20, "발매정보 리서치", "1.0", -1, null);
        addTimeLog(p20, "매입+판매 관리", "2.0", -2, null);

        // ══════════════════════════════════════════
        // 페르소나 21~30: 개발자 (DEVELOPER)
        // ══════════════════════════════════════════

        // 21. SaaS 개발자 (B2B)
        User u21 = createUser("dev01@test.com", encodedPw, "SaaS개발자김", BizType.DEVELOPER);
        Project p21 = createProject(u21, "B2B 재고관리 SaaS", "50000", "2000", "3000000", 200, "30000", true);
        addCost(p21, CostCategory.SERVER, "AWS 서버비", "FIXED", "500000", "EC2+RDS+S3");
        addCost(p21, CostCategory.API_USAGE, "외부 API 연동비", "VARIABLE", "1000", "건당 과금");
        addCost(p21, CostCategory.TOOL_SUBSCRIPTION, "GitHub+Jira", "FIXED", "50000", null);
        addTimeLog(p21, "백엔드 개발", "6.0", -1, "신규 기능");
        addTimeLog(p21, "인프라 관리", "2.0", -3, null);

        // 22. 모바일 앱 개발자
        User u22 = createUser("dev02@test.com", encodedPw, "앱개발자박", BizType.DEVELOPER);
        Project p22 = createProject(u22, "다이어트 트래킹 앱", "5000", "500", "1500000", 180, "25000", true);
        addCost(p22, CostCategory.SERVER, "Firebase", "FIXED", "100000", null);
        addCost(p22, CostCategory.API_USAGE, "ChatGPT API", "VARIABLE", "200", "식단 분석 건당");
        addCost(p22, CostCategory.OTHER, "앱스토어 개발자 등록", "FIXED", "10000", "연 12만/12");
        addTimeLog(p22, "앱 기능 개발", "5.0", -1, null);
        addTimeLog(p22, "버그 수정+QA", "3.0", -2, null);

        // 23. 프리랜서 웹 개발자
        User u23 = createUser("dev03@test.com", encodedPw, "프리랜서이", BizType.DEVELOPER);
        Project p23 = createProject(u23, "프리랜서 웹사이트 외주", "3000000", "100000", "500000", 160, "20000", false);
        addCost(p23, CostCategory.SERVER, "개발 서버", "FIXED", "30000", null);
        addCost(p23, CostCategory.TOOL_SUBSCRIPTION, "피그마 프로", "FIXED", "15000", null);
        addTimeLog(p23, "클라이언트 미팅", "2.0", -7, null);
        addTimeLog(p23, "프론트엔드 구현", "6.0", -1, null);
        addTimeLog(p23, "백엔드 구현", "6.0", -2, null);

        // 24. 워드프레스 테마 개발자
        User u24 = createUser("dev04@test.com", encodedPw, "테마개발자최", BizType.DEVELOPER);
        Project p24 = createProject(u24, "워드프레스 프리미엄 테마", "59000", "0", "200000", 120, "15000", true);
        addCost(p24, CostCategory.SERVER, "데모 서버", "FIXED", "30000", null);
        addCost(p24, CostCategory.MARKETING, "ThemeForest 수수료", "VARIABLE", "20000", "판매당 약 34%");
        addTimeLog(p24, "테마 개발", "5.0", -1, null);
        addTimeLog(p24, "고객 지원", "2.0", -1, "지원 티켓 처리");

        // 25. AI 챗봇 서비스 개발자
        User u25 = createUser("dev05@test.com", encodedPw, "AI개발자정", BizType.DEVELOPER);
        Project p25 = createProject(u25, "AI 고객응대 챗봇", "100000", "15000", "5000000", 200, "35000", true);
        addCost(p25, CostCategory.API_USAGE, "OpenAI API", "VARIABLE", "10000", "고객사당 월 토큰");
        addCost(p25, CostCategory.API_USAGE, "임베딩 DB", "VARIABLE", "5000", "Pinecone");
        addCost(p25, CostCategory.SERVER, "GCP 인프라", "FIXED", "800000", null);
        addCost(p25, CostCategory.OUTSOURCING, "디자인 외주", "FIXED", "500000", null);
        addTimeLog(p25, "모델 파인튜닝", "4.0", -3, null);
        addTimeLog(p25, "API 개발", "5.0", -1, null);

        // 26. 노코드 자동화 컨설턴트
        User u26 = createUser("dev06@test.com", encodedPw, "노코드컨설턴트강", BizType.DEVELOPER);
        Project p26 = createProject(u26, "Zapier 자동화 컨설팅", "500000", "50000", "300000", 80, "25000", false);
        addCost(p26, CostCategory.TOOL_SUBSCRIPTION, "Zapier 팀 플랜", "FIXED", "100000", null);
        addCost(p26, CostCategory.TOOL_SUBSCRIPTION, "Notion+Airtable", "FIXED", "30000", null);
        addTimeLog(p26, "고객 업무 분석", "3.0", -5, null);
        addTimeLog(p26, "자동화 구축", "5.0", -1, null);

        // 27. 게임 인디 개발자
        User u27 = createUser("dev07@test.com", encodedPw, "인디게임개발자윤", BizType.DEVELOPER);
        Project p27 = createProject(u27, "모바일 퍼즐 게임", "3000", "0", "800000", 200, "12000", true);
        addCost(p27, CostCategory.TOOL_SUBSCRIPTION, "Unity Pro", "FIXED", "50000", null);
        addCost(p27, CostCategory.OUTSOURCING, "사운드 외주", "FIXED", "200000", null);
        addCost(p27, CostCategory.MARKETING, "UA 광고비", "FIXED", "300000", "유저 획득");
        addTimeLog(p27, "게임 로직 개발", "6.0", -1, null);
        addTimeLog(p27, "레벨 디자인", "4.0", -2, null);

        // 28. 크롬 확장프로그램 개발자
        User u28 = createUser("dev08@test.com", encodedPw, "크롬확장개발자한", BizType.DEVELOPER);
        Project p28 = createProject(u28, "생산성 크롬 확장", "5000", "100", "100000", 60, "15000", true);
        addCost(p28, CostCategory.SERVER, "백엔드 서버", "FIXED", "30000", "Vercel Pro");
        addCost(p28, CostCategory.API_USAGE, "GPT API 사용료", "VARIABLE", "50", "요청당");
        addTimeLog(p28, "확장프로그램 개발", "4.0", -1, null);
        addTimeLog(p28, "크롬 스토어 관리", "1.0", -7, null);

        // 29. 데이터 분석 플랫폼 개발자
        User u29 = createUser("dev09@test.com", encodedPw, "데이터개발자서", BizType.DEVELOPER);
        Project p29 = createProject(u29, "마케팅 데이터 대시보드", "80000", "5000", "4000000", 200, "30000", false);
        addCost(p29, CostCategory.SERVER, "AWS 인프라", "FIXED", "1000000", "EMR+Redshift");
        addCost(p29, CostCategory.API_USAGE, "데이터 수집 API", "VARIABLE", "3000", "고객사당");
        addCost(p29, CostCategory.OUTSOURCING, "프론트엔드 외주", "FIXED", "800000", null);
        addTimeLog(p29, "데이터 파이프라인", "5.0", -1, null);
        addTimeLog(p29, "대시보드 기능 개발", "4.0", -2, null);

        // 30. API 마켓플레이스 개발자
        User u30 = createUser("dev10@test.com", encodedPw, "API개발자오", BizType.DEVELOPER);
        Project p30 = createProject(u30, "이미지 처리 API 서비스", "10000", "2000", "2000000", 160, "25000", true);
        addCost(p30, CostCategory.SERVER, "GPU 서버", "FIXED", "800000", "A100 인스턴스");
        addCost(p30, CostCategory.API_USAGE, "CDN 비용", "VARIABLE", "500", "GB당");
        addCost(p30, CostCategory.TOOL_SUBSCRIPTION, "모니터링 도구", "FIXED", "50000", "Datadog");
        addTimeLog(p30, "모델 최적화", "4.0", -3, null);
        addTimeLog(p30, "API 엔드포인트 개발", "5.0", -1, null);

        // ── 기존 커뮤니티 게시글 (호환성) ──
        BoardPost creatorPost = boardPostRepository.save(BoardPost.builder()
                .user(u1)
                .project(p1)
                .title("웹소설 작가의 손익분석 후기")
                .content("BEP가 18회분! 생각보다 적은 양으로 손익분기점 도달 가능하네요. "
                        + "실질 시급이 12,500원이라 최저시급보다 높아서 다행입니다.")
                .build());

        BoardPost sellerPost = boardPostRepository.save(BoardPost.builder()
                .user(u11)
                .project(p11)
                .title("핸드메이드 캔들 셀러의 BEP 분석 공유")
                .content("월 50개만 팔면 손익분기점 달성! 공헌이익 10,000원이 핵심입니다. "
                        + "재료비 비중이 높아서 대량구매로 원가 절감이 중요합니다.")
                .build());

        commentRepository.save(Comment.builder()
                .post(creatorPost)
                .user(u11)
                .content("작가님 분석 잘 보았습니다! 시간 투자 대비 수익률이 괜찮네요.")
                .build());

        commentRepository.save(Comment.builder()
                .post(sellerPost)
                .user(u1)
                .content("셀러분 공헌이익 분석 감사합니다! 비용 구조가 명확하니 좋습니다.")
                .build());

        chatLogRepository.save(ChatLog.builder()
                .user(u1)
                .project(p1)
                .question("이 프로젝트의 손익분기점이 궁금합니다")
                .answer("현재 프로젝트 '웹소설 연재 수익 분석'의 고정비는 50000원, "
                        + "판매가는 3000원, 변동비는 100원입니다. BEP ≈ 18회분입니다.")
                .build());

        // ── 트렌드 데모: 과거 6개월치 비용/시간 데이터 ──
        seedTrendData(p1, u1);
        seedTrendData(p11, u11);
        seedTrendData(p21, u21);

        // ══════════════════════════════════════════
        // 목표 추적 데이터 (대표 페르소나 3명)
        // ══════════════════════════════════════════
        p1.updateGoal(bd("500000"), "2026-06");   // 웹소설작가: 50만원 목표, 6월까지
        p11.updateGoal(bd("3000000"), "2026-08"); // 핸드메이드 양초: 300만원 목표, 8월까지
        p21.updateGoal(bd("10000000"), "2026-12"); // SaaS 개발자: 1000만원 목표, 12월까지

        log.info("[DataInitializer] 시드 데이터 생성 완료!");
        log.info("  관리자: admin@profitlogic.com / Test1234!");
        log.info("  페르소나: creator01~10, seller01~10, dev01~10 @test.com / Test1234!");
    }

    // ── 헬퍼 메서드 ──

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private User createUser(String email, String encodedPw, String nickname, BizType bizType) {
        return userRepository.save(User.builder()
                .email(email)
                .password(encodedPw)
                .nickname(nickname)
                .bizType(bizType)
                .build());
    }

    private Project createProject(User user, String title,
                                  String price, String variableCost, String fixedCost,
                                  int workHours, String hourlyWage, boolean isPublic) {
        return projectRepository.save(Project.builder()
                .user(user)
                .title(title)
                .price(bd(price))
                .variableCost(bd(variableCost))
                .fixedCost(bd(fixedCost))
                .workHours(workHours)
                .hourlyWage(bd(hourlyWage))
                .isPublic(isPublic)
                .build());
    }

    private void addCost(Project project, CostCategory category,
                         String costName, String costType, String amount, String memo) {
        costDetailRepository.save(CostDetail.builder()
                .project(project)
                .category(category)
                .costName(costName)
                .costType(costType)
                .amount(bd(amount))
                .memo(memo)
                .build());
    }

    private void addTimeLog(Project project, String taskName,
                            String hours, int daysAgo, String memo) {
        timeLogRepository.save(TimeLog.builder()
                .project(project)
                .taskName(taskName)
                .hoursSpent(bd(hours))
                .logDate(LocalDate.now().plusDays(daysAgo))
                .memo(memo)
                .build());
    }

    /**
     * 트렌드 차트 데모를 위한 과거 6개월치 비용/시간 데이터 생성.
     * 비용은 서서히 감소, 시간 효율은 개선되는 패턴.
     */
    private void seedTrendData(Project project, User user) {
        YearMonth current = YearMonth.now();
        // 고정비 변화 (서서히 감소): 1.3x → 1.2x → 1.1x → 1.05x → 1.0x → 0.95x
        double[] fixedMultipliers = {1.30, 1.20, 1.10, 1.05, 1.00, 0.95};
        // 시간 변화 (효율 개선): 1.4x → 1.3x → 1.2x → 1.1x → 1.05x → 1.0x
        double[] hoursMultipliers = {1.40, 1.30, 1.20, 1.10, 1.05, 1.00};

        BigDecimal baseFixed = project.getFixedCost();
        int baseHours = project.getWorkHours();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            LocalDate midMonth = ym.atDay(15);

            // 과거 비용 데이터 (FIXED)
            BigDecimal monthlyFixed = baseFixed.multiply(bd(String.valueOf(fixedMultipliers[5 - i])))
                    .setScale(0, java.math.RoundingMode.HALF_UP);
            CostDetail cost = costDetailRepository.save(CostDetail.builder()
                    .project(project)
                    .category(CostCategory.OTHER)
                    .costName(ym.getMonthValue() + "월 운영비")
                    .costType("FIXED")
                    .amount(monthlyFixed)
                    .memo("트렌드 데모 데이터")
                    .build());

            // created_at을 해당 월로 수정 (네이티브 쿼리)
            entityManager.flush();
            entityManager.createNativeQuery(
                    "UPDATE cost_details SET created_at = :date WHERE cost_detail_id = :id")
                    .setParameter("date", midMonth.atStartOfDay())
                    .setParameter("id", cost.getId())
                    .executeUpdate();

            // 과거 시간 데이터
            double monthlyHours = baseHours * hoursMultipliers[5 - i] / 4.0;
            for (int week = 0; week < 4; week++) {
                LocalDate logDate = ym.atDay(Math.min(1 + week * 7, ym.lengthOfMonth()));
                timeLogRepository.save(TimeLog.builder()
                        .project(project)
                        .taskName(ym.getMonthValue() + "월 " + (week + 1) + "주차 작업")
                        .hoursSpent(bd(String.format("%.1f", monthlyHours)))
                        .logDate(logDate)
                        .memo(null)
                        .build());
            }
        }
    }
}
