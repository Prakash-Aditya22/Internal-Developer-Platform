package com.idp.domain.entity;

import com.idp.domain.enums.EnvironmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "environments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class Environment extends BaseEntity {

  @Column(nullable = false)
  private String serviceName;

  @Column(nullable = false)
  private String gitRepo;

  @Column(nullable = false)
  private String branch;

  @Column
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EnvironmentStatus status;

  @Column
  private String containerId;

  @Column
  private String containerName;

  @Column
  private Integer exposedPort;

  @Column
  private String subdomain;

  @Column
  private String dockerImageId;

  @Column
  private String dockerImageTag;

  @Column(length = 2000)
  private String buildLogs;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @OneToMany(mappedBy = "environment", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Deployment> deployments = new ArrayList<>();

  @PrePersist
  public void prePersist() {
    if (status == null) {
      status = EnvironmentStatus.PENDING;
    }
    if (subdomain == null && serviceName != null) {
      subdomain = serviceName.toLowerCase().replaceAll("[^a-z0-9-]", "-") + ".dev.platform.local";
    }
  }

  public void addDeployment(Deployment deployment) {
    deployments.add(deployment);
    deployment.setEnvironment(this);
  }
}
