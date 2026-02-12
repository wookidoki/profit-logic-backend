package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.CostDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CostDetailRepository extends JpaRepository<CostDetail, Long> {

    List<CostDetail> findByProjectId(Long projectId);

    List<CostDetail> findByProjectIdAndCostType(Long projectId, String costType);

    List<CostDetail> findByProjectIdAndCreatedAtBetween(Long projectId,
                                                         LocalDateTime start, LocalDateTime end);

    void deleteByProjectId(Long projectId);
}
