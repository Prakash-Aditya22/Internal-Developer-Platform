package com.idp.repository;

import com.idp.domain.entity.Environment;
import com.idp.domain.entity.User;
import com.idp.domain.enums.EnvironmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long>,
    RevisionRepository<Environment, Long, Long> {

  Optional<Environment> findByServiceNameAndOwner(String serviceName, User owner);

  Optional<Environment> findByContainerId(String containerId);

  Optional<Environment> findBySubdomain(String subdomain);

  List<Environment> findByOwner(User owner);

  Page<Environment> findByOwner(User owner, Pageable pageable);

  List<Environment> findByStatus(EnvironmentStatus status);

  List<Environment> findByOwnerAndStatus(User owner, EnvironmentStatus status);

  @Query("SELECT e FROM Environment e WHERE e.owner.id = :ownerId")
  List<Environment> findAllByOwnerId(Long ownerId);

  @Query("SELECT COUNT(e) FROM Environment e WHERE e.status = :status")
  long countByStatus(EnvironmentStatus status);

  @Query("SELECT COUNT(e) FROM Environment e WHERE e.owner = :owner")
  long countByOwner(User owner);

  boolean existsByServiceNameAndOwner(String serviceName, User owner);

  @Query("SELECT e FROM Environment e WHERE e.status IN :statuses")
  List<Environment> findByStatusIn(List<EnvironmentStatus> statuses);
}
