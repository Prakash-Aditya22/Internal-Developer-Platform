package com.idp.mapper;

import com.idp.domain.entity.Deployment;
import com.idp.dto.response.DeploymentResponse;
import org.springframework.stereotype.Component;

@Component
public class DeploymentMapper {

  public DeploymentResponse toResponse(Deployment deployment) {
    return DeploymentResponse.builder()
        .id(deployment.getId())
        .environmentId(deployment.getEnvironment() != null ? deployment.getEnvironment().getId() : null)
        .environmentName(deployment.getEnvironment() != null ? deployment.getEnvironment().getServiceName() : null)
        .commitHash(deployment.getCommitHash())
        .commitMessage(deployment.getCommitMessage())
        .status(deployment.getStatus())
        .startedAt(deployment.getStartedAt())
        .completedAt(deployment.getCompletedAt())
        .durationSeconds(deployment.getDurationSeconds())
        .triggeredByUsername(deployment.getTriggeredBy() != null ? deployment.getTriggeredBy().getUsername() : null)
        .logs(deployment.getLogs())
        .createdAt(deployment.getCreatedAt())
        .build();
  }
}
