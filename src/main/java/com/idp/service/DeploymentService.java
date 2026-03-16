package com.idp.service;

import com.idp.domain.entity.Deployment;
import com.idp.domain.entity.Environment;
import com.idp.domain.entity.User;
import com.idp.domain.enums.DeploymentStatus;
import com.idp.domain.enums.EnvironmentStatus;
import com.idp.dto.request.DeployRequest;
import com.idp.dto.response.DeploymentResponse;
import com.idp.exception.ResourceNotFoundException;
import com.idp.mapper.DeploymentMapper;
import com.idp.repository.DeploymentRepository;
import com.idp.repository.EnvironmentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeploymentService {

  private final DeploymentRepository deploymentRepository;
  private final EnvironmentRepository environmentRepository;
  private final DockerService dockerService;
  private final GitService gitService;
  private final DeploymentMapper deploymentMapper;
  private final MeterRegistry meterRegistry;

  private Counter deploymentSuccessCounter;
  private Counter deploymentFailureCounter;
  private Timer deploymentTimer;

  @PostConstruct
  public void initMetrics() {
    deploymentSuccessCounter = Counter.builder("idp.deployments.success")
        .description("Number of successful deployments")
        .register(meterRegistry);
    deploymentFailureCounter = Counter.builder("idp.deployments.failure")
        .description("Number of failed deployments")
        .register(meterRegistry);
    deploymentTimer = Timer.builder("idp.deployment.duration")
        .description("Time taken to complete deployments")
        .register(meterRegistry);
  }

  @Transactional
  public DeploymentResponse triggerDeployment(Long environmentId, DeployRequest request, User user) {
    Environment environment = environmentRepository.findById(environmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + environmentId));

    Deployment deployment = Deployment.builder()
        .environment(environment)
        .commitHash(request.getCommitHash())
        .commitMessage(request.getCommitMessage())
        .status(DeploymentStatus.PENDING)
        .triggeredBy(user)
        .build();

    deployment = deploymentRepository.save(deployment);
    environment.addDeployment(deployment);

    // Execute deployment asynchronously
    executeDeploymentAsync(deployment.getId(), request.isForceRebuild());

    return deploymentMapper.toResponse(deployment);
  }

  @Async
  @Transactional
  public void executeDeploymentAsync(Long deploymentId, boolean forceRebuild) {
    Timer.Sample sample = Timer.start(meterRegistry);
    Deployment deployment = deploymentRepository.findById(deploymentId)
        .orElseThrow(() -> new ResourceNotFoundException("Deployment not found"));

    Environment environment = deployment.getEnvironment();
    StringBuilder logBuilder = new StringBuilder();

    try {
      // Update status to IN_PROGRESS
      deployment.setStatus(DeploymentStatus.IN_PROGRESS);
      deployment.setStartedAt(Instant.now());
      deploymentRepository.save(deployment);

      environment.setStatus(EnvironmentStatus.DEPLOYING);
      environmentRepository.save(environment);

      logBuilder.append("[").append(Instant.now()).append("] Starting deployment...\n");

      // Clone/update repository
      logBuilder.append("[").append(Instant.now()).append("] Cloning repository...\n");
      File workspaceDir = gitService.cloneRepository(environment.getGitRepo(), environment.getBranch());

      // Get commit info if not provided
      if (deployment.getCommitHash() == null) {
        String commitHash = gitService.getLatestCommitHash(workspaceDir);
        String commitMessage = gitService.getLatestCommitMessage(workspaceDir);
        deployment.setCommitHash(commitHash);
        deployment.setCommitMessage(commitMessage);
        logBuilder.append("[").append(Instant.now()).append("] Commit: ").append(commitHash).append("\n");
      }

      // Build Docker image
      logBuilder.append("[").append(Instant.now()).append("] Building Docker image...\n");
      environment.setStatus(EnvironmentStatus.BUILDING);
      environmentRepository.save(environment);

      String imageTag = environment.getServiceName() + ":" + deployment.getCommitHash().substring(0, 7);
      String imageId = dockerService.buildImage(workspaceDir, environment.getServiceName(),
          deployment.getCommitHash().substring(0, 7));

      deployment.setDockerImageId(imageId);
      environment.setDockerImageId(imageId);
      environment.setDockerImageTag(imageTag);
      logBuilder.append("[").append(Instant.now()).append("] Image built: ").append(imageId).append("\n");

      // Stop and remove old container if exists
      if (environment.getContainerId() != null) {
        logBuilder.append("[").append(Instant.now()).append("] Stopping old container...\n");
        try {
          dockerService.stopContainer(environment.getContainerId());
          dockerService.removeContainer(environment.getContainerId(), true);
        } catch (Exception e) {
          logBuilder.append("[").append(Instant.now()).append("] Warning: ").append(e.getMessage()).append("\n");
        }
      }

      // Create and start new container
      logBuilder.append("[").append(Instant.now()).append("] Creating container...\n");
      String containerName = environment.getServiceName() + "-" + deployment.getCommitHash().substring(0, 7);
      int hostPort = environment.getExposedPort() != null ? environment.getExposedPort() : findAvailablePort();

      Map<String, String> labels = new HashMap<>();
      labels.put("idp.environment.id", environment.getId().toString());
      labels.put("idp.service.name", environment.getServiceName());
      labels.put("idp.deployment.id", deployment.getId().toString());

      Map<String, String> envVars = new HashMap<>();
      envVars.put("SERVICE_NAME", environment.getServiceName());
      envVars.put("DEPLOYMENT_ID", deployment.getId().toString());

      String containerId = dockerService.createContainer(imageId, containerName, hostPort, 8080, envVars, labels);

      logBuilder.append("[").append(Instant.now()).append("] Starting container...\n");
      dockerService.startContainer(containerId);

      // Update environment
      environment.setContainerId(containerId);
      environment.setContainerName(containerName);
      environment.setExposedPort(hostPort);
      environment.setStatus(EnvironmentStatus.RUNNING);
      environmentRepository.save(environment);

      deployment.setContainerId(containerId);
      deployment.setStatus(DeploymentStatus.SUCCESS);
      deployment.setCompletedAt(Instant.now());
      logBuilder.append("[").append(Instant.now()).append("] Deployment completed successfully!\n");
      logBuilder.append("[").append(Instant.now()).append("] Service available at: http://")
          .append(environment.getSubdomain()).append(":").append(hostPort).append("\n");

      deploymentSuccessCounter.increment();

      // Clean up workspace
      gitService.cleanWorkspace(workspaceDir);

    } catch (Exception e) {
      log.error("Deployment failed for environment {}: {}", environment.getId(), e.getMessage(), e);
      deployment.setStatus(DeploymentStatus.FAILED);
      deployment.setCompletedAt(Instant.now());
      environment.setStatus(EnvironmentStatus.FAILED);
      logBuilder.append("[").append(Instant.now()).append("] ERROR: ").append(e.getMessage()).append("\n");
      deploymentFailureCounter.increment();
      environmentRepository.save(environment);
    } finally {
      deployment.setLogs(logBuilder.toString());
      deploymentRepository.save(deployment);
      sample.stop(deploymentTimer);
    }
  }

  private int findAvailablePort() {
    // Simple port allocation - in production, use a port manager
    return 8080 + (int) (Math.random() * 1000);
  }

  @Transactional(readOnly = true)
  public DeploymentResponse getDeployment(Long id) {
    Deployment deployment = deploymentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Deployment not found with id: " + id));
    return deploymentMapper.toResponse(deployment);
  }

  @Transactional(readOnly = true)
  public List<DeploymentResponse> getDeploymentsByEnvironment(Long environmentId) {
    return deploymentRepository.findByEnvironmentId(environmentId).stream()
        .map(deploymentMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public Page<DeploymentResponse> getDeploymentsByUser(Long userId, Pageable pageable) {
    return deploymentRepository.findByEnvironmentOwnerId(userId, pageable)
        .map(deploymentMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public String getDeploymentLogs(Long deploymentId) {
    Deployment deployment = deploymentRepository.findById(deploymentId)
        .orElseThrow(() -> new ResourceNotFoundException("Deployment not found with id: " + deploymentId));

    // Get live logs if container is running
    if (deployment.getContainerId() != null && deployment.getStatus() == DeploymentStatus.IN_PROGRESS) {
      return deployment.getLogs() + "\n--- Live Container Logs ---\n" +
          dockerService.getContainerLogs(deployment.getContainerId(), 100);
    }

    return deployment.getLogs();
  }

  @Transactional
  public void cancelDeployment(Long deploymentId) {
    Deployment deployment = deploymentRepository.findById(deploymentId)
        .orElseThrow(() -> new ResourceNotFoundException("Deployment not found with id: " + deploymentId));

    if (deployment.getStatus() == DeploymentStatus.IN_PROGRESS ||
        deployment.getStatus() == DeploymentStatus.PENDING) {
      deployment.setStatus(DeploymentStatus.CANCELLED);
      deployment.setCompletedAt(Instant.now());
      deploymentRepository.save(deployment);

      // Stop container if running
      if (deployment.getContainerId() != null) {
        try {
          dockerService.stopContainer(deployment.getContainerId());
        } catch (Exception e) {
          log.warn("Failed to stop container during cancellation: {}", e.getMessage());
        }
      }
    }
  }

  @Transactional(readOnly = true)
  public long countDeploymentsToday() {
    return deploymentRepository.countDeploymentsSince(
        Instant.now().minusSeconds(24 * 60 * 60));
  }

  @Transactional(readOnly = true)
  public Double getAverageDeploymentDuration() {
    return deploymentRepository.getAverageDeploymentDuration();
  }
}
