package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.BoardPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardPostRepository extends JpaRepository<BoardPost, Long> {

    Page<BoardPost> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
