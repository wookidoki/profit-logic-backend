package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByProjectIdOrderByYearMonthDesc(Long projectId);

    Optional<Report> findByProjectIdAndYearMonth(Long projectId, String yearMonth);

    void deleteByProjectId(Long projectId);
}
