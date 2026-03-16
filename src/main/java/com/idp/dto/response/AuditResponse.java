package com.idp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditResponse {

  private Long revisionNumber;
  private Instant revisionDate;
  private String revisionType;
  private String modifiedBy;
  private Object entity;
}
