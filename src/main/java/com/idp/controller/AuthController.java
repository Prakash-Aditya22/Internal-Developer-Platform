package com.idp.controller;

import com.idp.dto.request.LoginRequest;
import com.idp.dto.request.RegisterRequest;
import com.idp.dto.response.ApiResponse;
import com.idp.dto.response.AuthResponse;
import com.idp.dto.response.UserResponse;
import com.idp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and user registration APIs")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @Operation(summary = "Register a new user")
  public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
    log.info("Registration request for user: {}", request.getUsername());
    AuthResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, "User registered successfully"));
  }

  @PostMapping("/login")
  @Operation(summary = "Authenticate user and get JWT token")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    log.info("Login request for user: {}", request.getUsername());
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
  }

  @GetMapping("/me")
  @Operation(summary = "Get current authenticated user")
  public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
    UserResponse user = authService.getCurrentUser();
    return ResponseEntity.ok(ApiResponse.success(user));
  }
}
