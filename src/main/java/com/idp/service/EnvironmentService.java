package com.idp.service;

import com.idp.domain.entity.Environment;
import com.idp.domain.entity.User;
import com.idp.domain.enums.EnvironmentStatus;
import com.idp.dto.request.CreateEnvironmentRequest;
import com.idp.dto.request.DeployRequest;
import com.idp.dto.request.UpdateEnvironmentRequest;
import com.idp.dto.response.EnvironmentResponse;
import com.idp.exception.BadRequestException;
import com.idp.exception.ResourceNotFoundException;
import com.idp.mapper.EnvironmentMapper;
import com.idp.repository.EnvironmentRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnvironmentService {

  private final EnvironmentRepository environmentRepository;
  private final EnvironmentMapper environmentMapper;
  private final DeploymentService deploymentService;
  private final DockerService dockerService;
  private final MeterRegistry meterRegistry;

  @PostConstruct
  public void initMetrics() {
    Gauge.builder("idp.environments.total", environmentRepository, repo -> repo.count())
        .description("Total number of environments")
        .register(meterRegistry);

    Gauge.builder("idp.environments.running", () -> environmentRepository.countByStatus(EnvironmentStatus.RUNNING))
        .description("Number of running environments")
        .register(meterRegistry);

    Gauge.builder("idp.environments.failed", () -> environmentRepository.countByStatus(EnvironmentStatus.FAILED))
        .description("Number of failed environments")
        .register(meterRegistry);
  }

  @Transactional
  public EnvironmentResponse createEnvironment(CreateEnvironmentRequest request, User owner) {
    log.info("Creating environment '{}' for user '{}'", request.getServiceName(), owner.getUsername());

    // Check if environment with same name already exists for this user
    if (environmentRepository.existsByServiceNameAndOwner(request.getServiceName(), owner)) {
      throw new BadRequestException("Environment with name '" + request.getServiceName() + "' already exists");
    }

    Environment environment = environmentMapper.toEntity(request);
    environment.setOwner(owner);
    environment.setStatus(EnvironmentStatus.PENDING);

    environment = environmentRepository.save(environment);
    log.info("Environment '{}' created with ID: {}", environment.getServiceName(), environment.getId());

    // Trigger initial deployment
    DeployRequest deployRequest = DeployRequest.builder()
        .forceRebuild(true)
        .build();
    deploymentService.triggerDeployment(environment.getId(), deployRequest, owner);

    return environmentMapper.toResponse(environment);
  }

  @Transactional(readOnly = true)
  public EnvironmentResponse getEnvironment(Long id) {
    Environment environment = environmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + id));
    return environmentMapper.toResponse(environment);
  }

  @Transactional(readOnly = true)
  public EnvironmentResponse getEnvironmentByServiceName(String serviceName, User owner) {
    Environment environment = environmentRepository.findByServiceNameAndOwner(serviceName, owner)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Environment not found with name: " + serviceName));
    return environmentMapper.toResponse(environment);
  }

  @Transactional(readOnly = true)
  public List<EnvironmentResponse> getAllEnvironments() {
    return environmentRepository.findAll().stream()
        .map(environmentMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public Page<EnvironmentResponse> getEnvironmentsByOwner(User owner, Pageable pageable) {
    return environmentRepository.findByOwner(owner, pageable)
        .map(environmentMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public List<EnvironmentResponse> getEnvironmentsByOwner(User owner) {
    return environmentRepository.findByOwner(owner).stream()
        .map(environmentMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<EnvironmentResponse> getEnvironmentsByStatus(EnvironmentStatus status) {
    return environmentRepository.findByStatus(status).stream()
        .map(environmentMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public EnvironmentResponse updateEnvironment(Long id, UpdateEnvironmentRequest request, User user) {
    Environment environment = environmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + id));

    // Check ownership or admin role
    if (!environment.getOwner().getId().equals(user.getId()) && !user.hasRole("PLATFORM_ADMIN")) {
      throw new BadRequestException("You don't have permission to update this environment");
    }

    if (request.getBranch() != null) {
      environment.setBranch(request.getBranch());
    }
    if (request.getDescription() != null) {
      environment.setDescription(request.getDescription());
    }
    if (request.getExposedPort() != null) {
      environment.setExposedPort(request.getExposedPort());
    }

    environment = environmentRepository.save(environment);
    log.info("Environment '{}' updated", environment.getServiceName());

    return environmentMapper.toResponse(environment);
  }

  @Transactional
  public void deleteEnvironment(Long id, User user) {
    Environment environment = environmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + id));

    // Check ownership or admin role
    if (!environment.getOwner().getId().equals(user.getId()) && !user.hasRole("PLATFORM_ADMIN")) {
      throw new BadRequestException("You don't have permission to delete this environment");
    }

    log.info("Deleting environment '{}' (ID: {})", environment.getServiceName(), id);

    // Stop and remove container
    if (environment.getContainerId() != null) {
      try {
        dockerService.stopContainer(environment.getContainerId());
        dockerService.removeContainer(environment.getContainerId(), true);
      } catch (Exception e) {
        log.warn("Failed to remove container: {}", e.getMessage());
      }
    }

    // Remove image
    if (environment.getDockerImageId() != null) {
      try {
        dockerService.removeImage(environment.getDockerImageId());
      } catch (Exception e) {
        log.warn("Failed to remove image: {}", e.getMessage());
      }
    }

    environment.setStatus(EnvironmentStatus.DELETED);
    environmentRepository.save(environment);

    log.info("Environment '{}' deleted successfully", environment.getServiceName());
  }

  @Transactional
  public EnvironmentResponse startEnvironment(Long id, User user) {
    Environment environment = environmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + id));

    if (environment.getStatus() == EnvironmentStatus.RUNNING) {
      throw new BadRequestException("Environment is already running");
    }

    if (environment.getContainerId() != null) {
      dockerService.startContainer(environment.getContainerId());
      environment.setStatus(EnvironmentStatus.RUNNING);
      environmentRepository.save(environment);
    } else {
      // Trigger new deployment
      DeployRequest deployRequest = DeployRequest.builder().build();
      deploymentService.triggerDeployment(id, deployRequest, user);
    }

    return environmentMapper.toResponse(environment);
  }

  @Transactional
  public EnvironmentResponse stopEnvironment(Long id, User user) {
    Environment environment = environmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + id));

    if (environment.getStatus() != EnvironmentStatus.RUNNING) {
      throw new BadRequestException("Environment is not running");
    }

    if (environment.getContainerId() != null) {
      dockerService.stopContainer(environment.getContainerId());
    }

    environment.setStatus(EnvironmentStatus.STOPPED);
    environmentRepository.save(environment);

    return environmentMapper.toResponse(environment);
  }

  @Transactional
  public EnvironmentResponse restartEnvironment(Long id, User user) {
    stopEnvironment(id, user);
    return startEnvironment(id, user);
  }

  @Transactional(readOnly = true)
  public String getEnvironmentLogs(Long id, int tailLines) {
    Environment environment = environmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + id));

    if (environment.getContainerId() == null) {
      return "No container associated with this environment";
    }

    return dockerService.getContainerLogs(environment.getContainerId(), tailLines);
  }

  @Transactional(readOnly = true)
  public List<Revision<Long, Environment>> getEnvironmentHistory(Long id) {
    Revisions<Long, Environment> revisions = environmentRepository.findRevisions(id);
    return revisions.getContent();
  }

  @Transactional(readOnly = true)
  public long countEnvironmentsByOwner(User owner) {
    return environmentRepository.countByOwner(owner);
  }
}
