package com.idp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformMetricsResponse {

  private long totalEnvironments;
  private long runningEnvironments;
  private long failedEnvironments;
  private long totalDeployments;
  private long successfulDeployments;
  private long failedDeployments;
  private long deploymentsToday;
  private double averageDeploymentDurationSeconds;
  private long activeUsers;
}
