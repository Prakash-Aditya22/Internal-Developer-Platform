package com.idp.domain.entity;

import com.idp.domain.enums.DeploymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.time.Instant;

@Entity
@Table(name = "deployments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class Deployment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "environment_id", nullable = false)
  private Environment environment;

  @Column(nullable = false)
  private String commitHash;

  @Column
  private String commitMessage;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeploymentStatus status;

  @Column
  private Instant startedAt;

  @Column
  private Instant completedAt;

  @Column(length = 5000)
  private String logs;

  @Column
  private String dockerImageId;

  @Column
  private String containerId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "triggered_by_id")
  private User triggeredBy;

  @PrePersist
  public void prePersist() {
    if (status == null) {
      status = DeploymentStatus.PENDING;
    }
    if (startedAt == null) {
      startedAt = Instant.now();
    }
  }

  public long getDurationSeconds() {
    if (startedAt == null)
      return 0;
    Instant end = completedAt != null ? completedAt : Instant.now();
    return end.getEpochSecond() - startedAt.getEpochSecond();
  }
}
