package com.idp.dto.response;

import com.idp.domain.enums.DeploymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentResponse {

  private Long id;
  private Long environmentId;
  private String environmentName;
  private String commitHash;
  private String commitMessage;
  private DeploymentStatus status;
  private Instant startedAt;
  private Instant completedAt;
  private long durationSeconds;
  private String triggeredByUsername;
  private String logs;
  private Instant createdAt;
}
