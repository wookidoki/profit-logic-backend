package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

    List<TimeLog> findByProjectId(Long projectId);

    List<TimeLog> findByProjectIdOrderByLogDateDesc(Long projectId);

    List<TimeLog> findByProjectIdAndLogDateBetween(Long projectId, LocalDate start, LocalDate end);

    void deleteByProjectId(Long projectId);
}
