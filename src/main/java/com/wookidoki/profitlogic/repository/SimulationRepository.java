package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.Simulation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {

    List<Simulation> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
