package com.idp.service;

import com.idp.domain.entity.Role;
import com.idp.domain.entity.User;
import com.idp.domain.enums.RoleName;
import com.idp.dto.request.LoginRequest;
import com.idp.dto.request.RegisterRequest;
import com.idp.dto.response.AuthResponse;
import com.idp.dto.response.UserResponse;
import com.idp.exception.BadRequestException;
import com.idp.mapper.UserMapper;
import com.idp.repository.RoleRepository;
import com.idp.repository.UserRepository;
import com.idp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthenticationManager authenticationManager;
  private final UserMapper userMapper;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    log.info("Registering new user: {}", request.getUsername());

    if (userRepository.existsByUsername(request.getUsername())) {
      throw new BadRequestException("Username already exists");
    }

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new BadRequestException("Email already exists");
    }

    // Get or create DEVELOPER role
    Role developerRole = roleRepository.findByName(RoleName.DEVELOPER)
        .orElseGet(() -> roleRepository.save(new Role(RoleName.DEVELOPER, "Standard developer role")));

    User user = User.builder()
        .username(request.getUsername())
        .email(request.getEmail())
        .password(passwordEncoder.encode(request.getPassword()))
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .enabled(true)
        .build();

    user.addRole(developerRole);
    user = userRepository.save(user);

    log.info("User registered successfully: {}", user.getUsername());

    String token = jwtTokenProvider.generateToken(user);
    UserResponse userResponse = userMapper.toResponse(user);

    return AuthResponse.of(token, jwtTokenProvider.getExpirationMs(), userResponse);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    log.info("User login attempt: {}", request.getUsername());

    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    User user = (User) authentication.getPrincipal();
    String token = jwtTokenProvider.generateToken(user);
    UserResponse userResponse = userMapper.toResponse(user);

    log.info("User logged in successfully: {}", user.getUsername());

    return AuthResponse.of(token, jwtTokenProvider.getExpirationMs(), userResponse);
  }

  @Transactional(readOnly = true)
  public UserResponse getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new BadRequestException("No authenticated user");
    }

    User user = (User) authentication.getPrincipal();
    return userMapper.toResponse(user);
  }

  @Transactional
  public void promoteToAdmin(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BadRequestException("User not found"));

    Role adminRole = roleRepository.findByName(RoleName.PLATFORM_ADMIN)
        .orElseGet(() -> roleRepository.save(new Role(RoleName.PLATFORM_ADMIN, "Platform administrator role")));

    user.addRole(adminRole);
    userRepository.save(user);

    log.info("User {} promoted to admin", user.getUsername());
  }

  @Transactional
  public void demoteFromAdmin(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BadRequestException("User not found"));

    Role adminRole = roleRepository.findByName(RoleName.PLATFORM_ADMIN)
        .orElseThrow(() -> new BadRequestException("Admin role not found"));

    user.removeRole(adminRole);
    userRepository.save(user);

    log.info("User {} demoted from admin", user.getUsername());
  }
}
