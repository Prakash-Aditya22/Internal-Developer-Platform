package com.idp.controller;

import com.idp.domain.entity.User;
import com.idp.dto.response.ApiResponse;
import com.idp.dto.response.DeploymentResponse;
import com.idp.service.DeploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/deployments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Deployments", description = "Deployment management APIs")
@SecurityRequirement(name = "bearerAuth")
public class DeploymentController {

  private final DeploymentService deploymentService;

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Get deployment by ID")
  public ResponseEntity<ApiResponse<DeploymentResponse>> getDeployment(@PathVariable Long id) {
    DeploymentResponse deployment = deploymentService.getDeployment(id);
    return ResponseEntity.ok(ApiResponse.success(deployment));
  }

  @GetMapping("/my")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Get my deployments", description = "Returns all deployments for the current user's environments")
  public ResponseEntity<ApiResponse<Page<DeploymentResponse>>> getMyDeployments(
      @AuthenticationPrincipal User user,
      Pageable pageable) {

    Page<DeploymentResponse> deployments = deploymentService.getDeploymentsByUser(user.getId(), pageable);
    return ResponseEntity.ok(ApiResponse.success(deployments));
  }

  @GetMapping("/{id}/logs")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Get deployment logs")
  public ResponseEntity<ApiResponse<String>> getDeploymentLogs(@PathVariable Long id) {
    String logs = deploymentService.getDeploymentLogs(id);
    return ResponseEntity.ok(ApiResponse.success(logs));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Cancel deployment")
  public ResponseEntity<ApiResponse<Void>> cancelDeployment(@PathVariable Long id) {
    deploymentService.cancelDeployment(id);
    return ResponseEntity.ok(ApiResponse.success(null, "Deployment cancelled"));
  }
}
