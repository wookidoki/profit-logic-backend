package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

    List<TimeLog> findByProjectId(Long projectId);

    List<TimeLog> findByProjectIdOrderByLogDateDesc(Long projectId);

    List<TimeLog> findByProjectIdAndLogDateBetween(Long projectId, LocalDate start, LocalDate end);

    @Query("SELECT t FROM TimeLog t WHERE t.project.user.id = :userId AND t.logDate = :date")
    List<TimeLog> findByUserIdAndLogDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    void deleteByProjectId(Long projectId);
}
