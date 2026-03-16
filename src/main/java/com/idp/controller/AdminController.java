package com.idp.controller;

import com.idp.dto.response.ApiResponse;
import com.idp.dto.response.PlatformMetricsResponse;
import com.idp.service.AuthService;
import com.idp.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Platform administration APIs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminController {

  private final MetricsService metricsService;
  private final AuthService authService;

  @GetMapping("/metrics")
  @Operation(summary = "Get platform metrics", description = "Returns aggregated platform metrics for administrators")
  public ResponseEntity<ApiResponse<PlatformMetricsResponse>> getPlatformMetrics() {
    PlatformMetricsResponse metrics = metricsService.getPlatformMetrics();
    return ResponseEntity.ok(ApiResponse.success(metrics));
  }

  @PostMapping("/users/{userId}/promote")
  @Operation(summary = "Promote user to admin")
  public ResponseEntity<ApiResponse<Void>> promoteToAdmin(@PathVariable Long userId) {
    authService.promoteToAdmin(userId);
    return ResponseEntity.ok(ApiResponse.success(null, "User promoted to admin"));
  }

  @PostMapping("/users/{userId}/demote")
  @Operation(summary = "Demote user from admin")
  public ResponseEntity<ApiResponse<Void>> demoteFromAdmin(@PathVariable Long userId) {
    authService.demoteFromAdmin(userId);
    return ResponseEntity.ok(ApiResponse.success(null, "User demoted from admin"));
  }
}
