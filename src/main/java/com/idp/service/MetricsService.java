package com.idp.service;

import com.idp.domain.enums.DeploymentStatus;
import com.idp.domain.enums.EnvironmentStatus;
import com.idp.dto.response.PlatformMetricsResponse;
import com.idp.repository.DeploymentRepository;
import com.idp.repository.EnvironmentRepository;
import com.idp.repository.UserRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsService {

  private final EnvironmentRepository environmentRepository;
  private final DeploymentRepository deploymentRepository;
  private final UserRepository userRepository;

  @Timed(value = "idp.metrics.platform", description = "Time taken to gather platform metrics")
  @Transactional(readOnly = true)
  public PlatformMetricsResponse getPlatformMetrics() {
    long totalEnvironments = environmentRepository.count();
    long runningEnvironments = environmentRepository.countByStatus(EnvironmentStatus.RUNNING);
    long failedEnvironments = environmentRepository.countByStatus(EnvironmentStatus.FAILED);

    long totalDeployments = deploymentRepository.count();
    long successfulDeployments = deploymentRepository.countByStatus(DeploymentStatus.SUCCESS);
    long failedDeployments = deploymentRepository.countByStatus(DeploymentStatus.FAILED);

    Instant dayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
    long deploymentsToday = deploymentRepository.countDeploymentsSince(dayAgo);

    Double avgDuration = deploymentRepository.getAverageDeploymentDuration();
    long activeUsers = userRepository.countActiveUsers();

    return PlatformMetricsResponse.builder()
        .totalEnvironments(totalEnvironments)
        .runningEnvironments(runningEnvironments)
        .failedEnvironments(failedEnvironments)
        .totalDeployments(totalDeployments)
        .successfulDeployments(successfulDeployments)
        .failedDeployments(failedDeployments)
        .deploymentsToday(deploymentsToday)
        .averageDeploymentDurationSeconds(avgDuration != null ? avgDuration : 0.0)
        .activeUsers(activeUsers)
        .build();
  }
}
