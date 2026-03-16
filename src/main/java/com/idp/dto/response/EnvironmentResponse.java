package com.idp.dto.response;

import com.idp.domain.enums.EnvironmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentResponse {

  private Long id;
  private String serviceName;
  private String gitRepo;
  private String branch;
  private String description;
  private EnvironmentStatus status;
  private String containerId;
  private String containerName;
  private Integer exposedPort;
  private String subdomain;
  private String serviceUrl;
  private String dockerImageTag;
  private String ownerUsername;
  private Long ownerId;
  private Instant createdAt;
  private Instant updatedAt;
  private String createdBy;
  private int deploymentCount;
}
