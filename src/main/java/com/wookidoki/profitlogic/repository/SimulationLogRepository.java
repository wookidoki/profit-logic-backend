package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.SimulationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationLogRepository extends JpaRepository<SimulationLog, Long> {

    List<SimulationLog> findByProjectId(Long projectId);
}
