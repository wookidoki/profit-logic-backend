package com.wookidoki.profitlogic.repository;

import com.wookidoki.profitlogic.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByUserId(Long userId);

    @Query("SELECT p.user.id, COUNT(p) FROM Project p GROUP BY p.user.id")
    List<Object[]> countProjectsGroupByUserId();
}
