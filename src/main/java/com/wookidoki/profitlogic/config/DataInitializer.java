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

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("local")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final SimulationRepository simulationRepository;
    private final BoardPostRepository boardPostRepository;
    private final CommentRepository commentRepository;
    private final ChatLogRepository chatLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail("creator@test.com")) {
            log.info("[DataInitializer] 시드 데이터 이미 존재 → 스킵");
            return;
        }

        log.info("[DataInitializer] 시드 데이터 생성 시작...");

        // ── Type A: 시간 투자형 (웹소설 작가) ──
        User creator = userRepository.save(User.builder()
                .email("creator@test.com")
                .password(passwordEncoder.encode("test1234"))
                .nickname("웹소설작가김")
                .bizType(BizType.CREATOR)
                .build());

        Project creatorProject = projectRepository.save(Project.builder()
                .user(creator)
                .title("웹소설 연재 프로젝트")
                .price(new BigDecimal("3000"))
                .variableCost(new BigDecimal("100"))
                .fixedCost(new BigDecimal("50000"))
                .workHours(160)
                .hourlyWage(new BigDecimal("9860"))
                .isPublic(true)
                .build());

        simulationRepository.save(Simulation.builder()
                .project(creatorProject)
                .scenarioName("기본 시나리오")
                .resultJson("{\"break_even_point\":18,\"target_quantity\":706.90,"
                        + "\"operating_profit\":2000010.00,\"economic_profit\":422410.00,"
                        + "\"shadow_wage\":12500.06,\"margin_rate\":97.45,\"is_viable\":true}")
                .build());

        // ── Type B: 비용 지출형 (쇼핑몰 셀러) ──
        User seller = userRepository.save(User.builder()
                .email("seller@test.com")
                .password(passwordEncoder.encode("test1234"))
                .nickname("쇼핑몰셀러박")
                .bizType(BizType.SELLER)
                .build());

        Project sellerProject = projectRepository.save(Project.builder()
                .user(seller)
                .title("쇼핑몰 운영 프로젝트")
                .price(new BigDecimal("25000"))
                .variableCost(new BigDecimal("12000"))
                .fixedCost(new BigDecimal("1500000"))
                .workHours(200)
                .hourlyWage(new BigDecimal("9860"))
                .isPublic(true)
                .build());

        simulationRepository.save(Simulation.builder()
                .project(sellerProject)
                .scenarioName("원가 절감 시나리오")
                .resultJson("{\"break_even_point\":116,\"target_quantity\":500.00,"
                        + "\"operating_profit\":5000000.00,\"economic_profit\":3028000.00,"
                        + "\"shadow_wage\":25000.00,\"margin_rate\":76.80,\"is_viable\":true}")
                .build());

        // ── 커뮤니티 게시글 ──
        BoardPost creatorPost = boardPostRepository.save(BoardPost.builder()
                .user(creator)
                .project(creatorProject)
                .title("웹소설 작가의 손익분석 후기")
                .content("BEP가 18회분! 생각보다 적은 양으로 손익분기점 도달 가능하네요. "
                        + "실질 시급이 12,500원이라 최저시급보다 높아서 다행입니다. "
                        + "Shadow Wage 지표가 정말 유용합니다.")
                .build());

        BoardPost sellerPost = boardPostRepository.save(BoardPost.builder()
                .user(seller)
                .project(sellerProject)
                .title("쇼핑몰 셀러의 BEP 분석 공유")
                .content("월 116개만 팔면 손익분기점 달성! 공헌이익 13,000원이 핵심입니다. "
                        + "목표이익 500만원을 위해서는 월 500개 판매가 필요하네요.")
                .build());

        // ── 서로의 글에 댓글 ──
        commentRepository.save(Comment.builder()
                .post(creatorPost)
                .user(seller)
                .content("작가님 분석 잘 보았습니다! 시간 투자 대비 수익률이 괜찮네요.")
                .build());

        commentRepository.save(Comment.builder()
                .post(sellerPost)
                .user(creator)
                .content("셀러분 공헌이익 분석 감사합니다! 비용 구조가 명확하니 좋습니다.")
                .build());

        // ── 챗봇 로그 ──
        chatLogRepository.save(ChatLog.builder()
                .user(creator)
                .project(creatorProject)
                .question("이 프로젝트의 손익분기점이 궁금합니다")
                .answer("현재 프로젝트 '웹소설 연재 프로젝트'의 고정비는 50000원, "
                        + "판매가는 3000원, 변동비는 100원입니다. "
                        + "손익분기점 분석을 위해 대시보드의 분석 기능을 이용해보세요.")
                .build());

        chatLogRepository.save(ChatLog.builder()
                .user(seller)
                .project(sellerProject)
                .question("이 쇼핑몰의 수익성은 어떤가요?")
                .answer("프로젝트 '쇼핑몰 운영 프로젝트'의 판매가 25000원에서 변동비 12000원을 빼면 "
                        + "단위당 공헌이익은 13000원입니다. 자세한 수익 분석은 대시보드를 확인해주세요.")
                .build());

        log.info("[DataInitializer] 시드 데이터 생성 완료! (creator@test.com / seller@test.com)");
    }
}
