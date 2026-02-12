package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.AdminStatsResponse;
import com.wookidoki.profitlogic.dto.AdminUserResponse;
import com.wookidoki.profitlogic.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final BoardPostRepository boardPostRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final ChatLogRepository chatLogRepository;
    private final CostDetailRepository costDetailRepository;
    private final TimeLogRepository timeLogRepository;
    private final SimulationRepository simulationRepository;
    private final SimulationLogRepository simulationLogRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                .userCount(userRepository.count())
                .projectCount(projectRepository.count())
                .postCount(boardPostRepository.count())
                .commentCount(commentRepository.count())
                .reportCount(reportRepository.count())
                .chatCount(chatLogRepository.count())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(user -> AdminUserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .role(user.getRole().name())
                        .bizType(user.getBizType() != null ? user.getBizType().name() : null)
                        .projectCount(projectRepository.findByUserId(user.getId()).size())
                        .createdAt(user.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 사용자의 프로젝트에 연결된 데이터 삭제
        List<Long> projectIds = projectRepository.findByUserId(userId).stream()
                .map(p -> p.getId())
                .toList();

        for (Long projectId : projectIds) {
            simulationLogRepository.deleteByProjectId(projectId);
            simulationRepository.deleteByProjectId(projectId);
            costDetailRepository.deleteByProjectId(projectId);
            timeLogRepository.deleteByProjectId(projectId);
            chatLogRepository.deleteByProjectId(projectId);
            reportRepository.deleteByProjectId(projectId);
        }
        projectRepository.deleteAll(projectRepository.findByUserId(userId));

        // 사용자의 댓글 삭제
        commentRepository.deleteByUserId(userId);

        // 사용자의 게시글에 달린 댓글 삭제 후 게시글 삭제
        boardPostRepository.findByUserId(userId).forEach(post -> {
            commentRepository.deleteByPostId(post.getId());
        });
        boardPostRepository.deleteAll(boardPostRepository.findByUserId(userId));

        // 사용자 삭제
        userRepository.delete(user);
    }

    @Transactional
    public void deletePost(Long postId) {
        if (!boardPostRepository.existsById(postId)) {
            throw new EntityNotFoundException("게시글을 찾을 수 없습니다.");
        }
        commentRepository.deleteByPostId(postId);
        boardPostRepository.deleteById(postId);
    }
}
