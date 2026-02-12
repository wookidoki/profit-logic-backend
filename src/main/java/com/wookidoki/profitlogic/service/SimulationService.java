package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.Simulation;
import com.wookidoki.profitlogic.dto.simulation.SimulationCreateRequest;
import com.wookidoki.profitlogic.dto.simulation.SimulationResponse;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationRepository simulationRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public SimulationResponse save(Long userId, SimulationCreateRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", request.getProjectId()));

        if (!project.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }

        Simulation simulation = Simulation.builder()
                .project(project)
                .scenarioName(request.getScenarioName())
                .resultJson(request.getResultJson())
                .build();

        return SimulationResponse.from(simulationRepository.save(simulation));
    }

    @Transactional(readOnly = true)
    public List<SimulationResponse> getByProject(Long userId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", projectId));

        if (!project.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }

        return simulationRepository.findByProjectId(projectId).stream()
                .map(SimulationResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long simulationId, Long userId) {
        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("시뮬레이션", simulationId));

        if (!simulation.getProject().getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }

        simulationRepository.delete(simulation);
    }
}
