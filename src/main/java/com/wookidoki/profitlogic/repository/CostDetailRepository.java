package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.CostDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CostDetailRepository extends JpaRepository<CostDetail, Long> {

    List<CostDetail> findByProjectId(Long projectId);

    List<CostDetail> findByProjectIdAndCostType(Long projectId, String costType);

    void deleteByProjectId(Long projectId);
}
