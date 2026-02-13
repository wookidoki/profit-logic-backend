package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.ChatLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {

    List<ChatLog> findByUserIdAndProjectIdOrderByCreatedAtDesc(Long userId, Long projectId);

    List<ChatLog> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    long countByUserId(Long userId);

    void deleteByProjectId(Long projectId);
}
