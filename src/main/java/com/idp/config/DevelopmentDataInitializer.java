package com.idp.config;

import com.idp.domain.entity.Role;
import com.idp.domain.entity.User;
import com.idp.domain.enums.RoleName;
import com.idp.repository.RoleRepository;
import com.idp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DevelopmentDataInitializer implements ApplicationRunner {

  private final RoleRepository roleRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Role developerRole = roleRepository.findByName(RoleName.DEVELOPER)
        .orElseGet(() -> roleRepository.save(
            new Role(RoleName.DEVELOPER, "Standard developer role")));
    Role adminRole = roleRepository.findByName(RoleName.PLATFORM_ADMIN)
        .orElseGet(() -> roleRepository.save(
            new Role(RoleName.PLATFORM_ADMIN, "Platform administrator role")));

    if (!userRepository.existsByUsername("admin")) {
      User admin = User.builder()
          .username("admin")
          .email("admin@idp.local")
          .password(passwordEncoder.encode("admin123"))
          .firstName("Admin")
          .lastName("User")
          .build();
      admin.addRole(developerRole);
      admin.addRole(adminRole);
      userRepository.save(admin);
    }

    if (!userRepository.existsByUsername("developer")) {
      User developer = User.builder()
          .username("developer")
          .email("developer@idp.local")
          .password(passwordEncoder.encode("dev123"))
          .firstName("Dev")
          .lastName("User")
          .build();
      developer.addRole(developerRole);
      userRepository.save(developer);
    }
  }
}
