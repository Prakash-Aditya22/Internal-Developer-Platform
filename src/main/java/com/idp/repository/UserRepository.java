package com.idp.repository;

import com.idp.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>,
    RevisionRepository<User, Long, Long> {

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.username = :username")
  Optional<User> findByUsernameWithRoles(String username);

  @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'PLATFORM_ADMIN'")
  java.util.List<User> findAllAdmins();

  @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
  long countActiveUsers();
}
