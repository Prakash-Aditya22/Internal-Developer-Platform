package com.idp.repository;

import com.idp.domain.entity.Deployment;
import com.idp.domain.entity.Environment;
import com.idp.domain.enums.DeploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long>,
    RevisionRepository<Deployment, Long, Long> {

  List<Deployment> findByEnvironment(Environment environment);

  Page<Deployment> findByEnvironment(Environment environment, Pageable pageable);

  List<Deployment> findByEnvironmentOrderByCreatedAtDesc(Environment environment);

  Optional<Deployment> findTopByEnvironmentOrderByCreatedAtDesc(Environment environment);

  List<Deployment> findByStatus(DeploymentStatus status);

  @Query("SELECT d FROM Deployment d WHERE d.environment.id = :environmentId ORDER BY d.createdAt DESC")
  List<Deployment> findByEnvironmentId(Long environmentId);

  @Query("SELECT COUNT(d) FROM Deployment d WHERE d.status = :status")
  long countByStatus(DeploymentStatus status);

  @Query("SELECT COUNT(d) FROM Deployment d WHERE d.createdAt >= :since")
  long countDeploymentsSince(Instant since);

  @Query("SELECT d FROM Deployment d WHERE d.triggeredBy.id = :userId ORDER BY d.createdAt DESC")
  List<Deployment> findByTriggeredByUserId(Long userId);

  @Query("SELECT d FROM Deployment d WHERE d.environment.owner.id = :ownerId ORDER BY d.createdAt DESC")
  Page<Deployment> findByEnvironmentOwnerId(Long ownerId, Pageable pageable);

  @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, d.startedAt, d.completedAt)) FROM Deployment d WHERE d.status = 'SUCCESS' AND d.completedAt IS NOT NULL")
  Double getAverageDeploymentDuration();
}
