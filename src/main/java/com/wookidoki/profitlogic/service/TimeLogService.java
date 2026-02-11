package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.TimeLog;
import com.wookidoki.profitlogic.dto.TimeLogCreateRequest;
import com.wookidoki.profitlogic.dto.TimeLogResponse;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.TimeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeLogService {

    private final TimeLogRepository timeLogRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public TimeLogResponse create(Long userId, Long projectId, TimeLogCreateRequest request) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);

        TimeLog timeLog = TimeLog.builder()
                .project(project)
                .taskName(request.getTaskName())
                .hoursSpent(request.getHoursSpent())
                .logDate(request.getLogDate())
                .memo(request.getMemo())
                .build();

        return TimeLogResponse.from(timeLogRepository.save(timeLog));
    }

    @Transactional(readOnly = true)
    public List<TimeLogResponse> getByProject(Long userId, Long projectId) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);

        return timeLogRepository.findByProjectIdOrderByLogDateDesc(projectId).stream()
                .map(TimeLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimeLogResponse> getByProjectAndDateRange(Long userId, Long projectId,
                                                          LocalDate from, LocalDate to) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);

        return timeLogRepository.findByProjectIdAndLogDateBetween(projectId, from, to).stream()
                .map(TimeLogResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long timeLogId, Long userId) {
        TimeLog timeLog = timeLogRepository.findById(timeLogId)
                .orElseThrow(() -> new ResourceNotFoundException("시간 기록", timeLogId));

        if (!timeLog.getProject().getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }

        timeLogRepository.delete(timeLog);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalHours(Long userId, Long projectId) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);

        return timeLogRepository.findByProjectIdOrderByLogDateDesc(projectId).stream()
                .map(TimeLog::getHoursSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", projectId));
    }

    private void validateOwnership(Project project, Long userId) {
        if (!project.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }
    }
}
