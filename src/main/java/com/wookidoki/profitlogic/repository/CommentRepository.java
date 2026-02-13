package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    int countByPostId(Long postId);

    List<Comment> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    void deleteByPostId(Long postId);

    void deleteByUserId(Long userId);
}
