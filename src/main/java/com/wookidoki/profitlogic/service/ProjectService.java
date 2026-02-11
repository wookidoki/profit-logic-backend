package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.ProjectCreateRequest;
import com.wookidoki.profitlogic.dto.ProjectResponse;
import com.wookidoki.profitlogic.dto.ProjectUpdateRequest;
import com.wookidoki.profitlogic.repository.ChatLogRepository;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.SimulationRepository;
import com.wookidoki.profitlogic.repository.TimeLogRepository;
import com.wookidoki.profitlogic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SimulationRepository simulationRepository;
    private final ChatLogRepository chatLogRepository;
    private final TimeLogRepository timeLogRepository;

    @Transactional
    public ProjectResponse create(Long userId, ProjectCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자", userId));

        Project project = Project.builder()
                .user(user)
                .title(request.getTitle())
                .price(request.getPrice())
                .variableCost(request.getVariableCost())
                .fixedCost(request.getFixedCost())
                .workHours(request.getWorkHours())
                .hourlyWage(request.getHourlyWage())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .build();

        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getMyProjects(Long userId) {
        return projectRepository.findByUserId(userId).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long projectId, Long userId) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(Long projectId, Long userId, ProjectUpdateRequest request) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);

        project.update(
                request.getTitle(),
                request.getPrice(),
                request.getVariableCost(),
                request.getFixedCost(),
                request.getWorkHours(),
                request.getHourlyWage(),
                request.getIsPublic()
        );

        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long projectId, Long userId) {
        Project project = findProjectOrThrow(projectId);
        validateOwnership(project, userId);
        chatLogRepository.deleteByProjectId(projectId);
        simulationRepository.deleteByProjectId(projectId);
        timeLogRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
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
