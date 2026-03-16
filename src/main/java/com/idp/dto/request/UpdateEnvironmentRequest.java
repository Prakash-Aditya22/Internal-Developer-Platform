package com.idp.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEnvironmentRequest {

  private String branch;

  private String description;

  private String dockerfilePath;

  private Integer exposedPort;
}
