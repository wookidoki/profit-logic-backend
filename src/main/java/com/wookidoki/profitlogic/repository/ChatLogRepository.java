package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.ChatLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {

    List<ChatLog> findByUserIdAndProjectIdOrderByCreatedAtDesc(Long userId, Long projectId);
}
