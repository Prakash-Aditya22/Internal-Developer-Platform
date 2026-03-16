package com.idp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEnvironmentRequest {

  @NotBlank(message = "Service name is required")
  @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$", message = "Service name must be lowercase alphanumeric with hyphens")
  private String serviceName;

  @NotBlank(message = "Git repository URL is required")
  @Pattern(regexp = "^https?://.*\\.git$|^git@.*:.*\\.git$|^https://github\\.com/.*$", message = "Invalid Git repository URL")
  private String gitRepo;

  @NotBlank(message = "Branch is required")
  private String branch;

  private String description;

  private String dockerfilePath;

  private Integer exposedPort;
}
