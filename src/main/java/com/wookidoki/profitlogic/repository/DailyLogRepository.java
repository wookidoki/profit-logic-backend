package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {

    Optional<DailyLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);

    List<DailyLog> findByUserIdAndLogDateBetweenOrderByLogDateDesc(
            Long userId, LocalDate start, LocalDate end);
}
