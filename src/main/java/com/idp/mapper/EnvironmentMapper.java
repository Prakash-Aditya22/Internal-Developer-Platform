package com.idp.mapper;

import com.idp.domain.entity.Environment;
import com.idp.dto.request.CreateEnvironmentRequest;
import com.idp.dto.response.EnvironmentResponse;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentMapper {

  public Environment toEntity(CreateEnvironmentRequest request) {
    return Environment.builder()
        .serviceName(request.getServiceName())
        .gitRepo(request.getGitRepo())
        .branch(request.getBranch())
        .description(request.getDescription())
        .exposedPort(request.getExposedPort())
        .build();
  }

  public EnvironmentResponse toResponse(Environment environment) {
    return EnvironmentResponse.builder()
        .id(environment.getId())
        .serviceName(environment.getServiceName())
        .gitRepo(environment.getGitRepo())
        .branch(environment.getBranch())
        .description(environment.getDescription())
        .status(environment.getStatus())
        .containerId(environment.getContainerId())
        .containerName(environment.getContainerName())
        .exposedPort(environment.getExposedPort())
        .subdomain(environment.getSubdomain())
        .serviceUrl(buildServiceUrl(environment))
        .dockerImageTag(environment.getDockerImageTag())
        .ownerUsername(environment.getOwner() != null ? environment.getOwner().getUsername() : null)
        .ownerId(environment.getOwner() != null ? environment.getOwner().getId() : null)
        .createdAt(environment.getCreatedAt())
        .updatedAt(environment.getUpdatedAt())
        .createdBy(environment.getCreatedBy())
        .deploymentCount(environment.getDeployments() != null ? environment.getDeployments().size() : 0)
        .build();
  }

  private String buildServiceUrl(Environment environment) {
    if (environment.getExposedPort() != null && environment.getSubdomain() != null) {
      return "http://" + environment.getSubdomain() + ":" + environment.getExposedPort();
    }
    return null;
  }
}
