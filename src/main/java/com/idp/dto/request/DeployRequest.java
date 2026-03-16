package com.idp.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeployRequest {

  private String commitHash;

  private String commitMessage;

  private boolean forceRebuild;
}
