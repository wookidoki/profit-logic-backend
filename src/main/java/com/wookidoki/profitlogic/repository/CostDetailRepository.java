package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.CostDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CostDetailRepository extends JpaRepository<CostDetail, Long> {

    List<CostDetail> findByProjectId(Long projectId);

    List<CostDetail> findByProjectIdAndCostType(Long projectId, String costType);

    List<CostDetail> findByProjectIdAndCreatedAtBetween(Long projectId,
                                                         LocalDateTime start, LocalDateTime end);

    @Query("SELECT c FROM CostDetail c WHERE c.project.user.id = :userId AND c.createdAt BETWEEN :start AND :end")
    List<CostDetail> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    void deleteByProjectId(Long projectId);
}
