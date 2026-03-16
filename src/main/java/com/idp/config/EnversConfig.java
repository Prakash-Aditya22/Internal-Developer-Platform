package com.idp.config;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnversConfig {

  private final EntityManagerFactory entityManagerFactory;

  public EnversConfig(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  @Bean
  public AuditReader auditReader() {
    return AuditReaderFactory.get(entityManagerFactory.createEntityManager());
  }
}
