package com.idp.controller;

import com.idp.domain.entity.User;
import com.idp.dto.request.CreateEnvironmentRequest;
import com.idp.dto.request.DeployRequest;
import com.idp.dto.request.UpdateEnvironmentRequest;
import com.idp.dto.response.ApiResponse;
import com.idp.dto.response.DeploymentResponse;
import com.idp.dto.response.EnvironmentResponse;
import com.idp.service.DeploymentService;
import com.idp.service.EnvironmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/environments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Environments", description = "Environment management APIs")
@SecurityRequirement(name = "bearerAuth")
public class EnvironmentController {

  private final EnvironmentService environmentService;
  private final DeploymentService deploymentService;

  @PostMapping
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Create a new environment", description = "Provisions a new development environment from a Git repository")
  public ResponseEntity<ApiResponse<EnvironmentResponse>> createEnvironment(
      @Valid @RequestBody CreateEnvironmentRequest request,
      @AuthenticationPrincipal User user) {

    log.info("Creating environment for service: {}", request.getServiceName());
    EnvironmentResponse response = environmentService.createEnvironment(request, user);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, "Environment created successfully. Deployment in progress."));
  }

  @GetMapping
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Get all environments", description = "Returns environments based on user role")
  public ResponseEntity<ApiResponse<Page<EnvironmentResponse>>> getEnvironments(
      @AuthenticationPrincipal User user,
      Pageable pageable) {

    Page<EnvironmentResponse> environments;
    if (user.hasRole("PLATFORM_ADMIN")) {
      // Admins see all environments
      environments = environmentService.getAllEnvironments()
          .stream()
          .collect(java.util.stream.Collectors.collectingAndThen(
              java.util.stream.Collectors.toList(),
              list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())));
    } else {
      environments = environmentService.getEnvironmentsByOwner(user, pageable);
    }
    return ResponseEntity.ok(ApiResponse.success(environments));
  }

  @GetMapping("/my")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Get my environments", description = "Returns all environments owned by the current user")
  public ResponseEntity<ApiResponse<List<EnvironmentResponse>>> getMyEnvironments(
      @AuthenticationPrincipal User user) {

    List<EnvironmentResponse> environments = environmentService.getEnvironmentsByOwner(user);
    return ResponseEntity.ok(ApiResponse.success(environments));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Get environment by ID")
  public ResponseEntity<ApiResponse<EnvironmentResponse>> getEnvironment(@PathVariable Long id) {
    EnvironmentResponse environment = environmentService.getEnvironment(id);
    return ResponseEntity.ok(ApiResponse.success(environment));
  }

  @GetMapping("/name/{serviceName}")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Get environment by service name")
  public ResponseEntity<ApiResponse<EnvironmentResponse>> getEnvironmentByName(
      @PathVariable String serviceName,
      @AuthenticationPrincipal User user) {

    EnvironmentResponse environment = environmentService.getEnvironmentByServiceName(serviceName, user);
    return ResponseEntity.ok(ApiResponse.success(environment));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Update environment")
  public ResponseEntity<ApiResponse<EnvironmentResponse>> updateEnvironment(
      @PathVariable Long id,
      @Valid @RequestBody UpdateEnvironmentRequest request,
      @AuthenticationPrincipal User user) {

    EnvironmentResponse environment = environmentService.updateEnvironment(id, request, user);
    return ResponseEntity.ok(ApiResponse.success(environment, "Environment updated successfully"));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Delete environment")
  public ResponseEntity<ApiResponse<Void>> deleteEnvironment(
      @PathVariable Long id,
      @AuthenticationPrincipal User user) {

    environmentService.deleteEnvironment(id, user);
    return ResponseEntity.ok(ApiResponse.success(null, "Environment deleted successfully"));
  }

  @PostMapping("/{id}/start")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Start environment")
  public ResponseEntity<ApiResponse<EnvironmentResponse>> startEnvironment(
      @PathVariable Long id,
      @AuthenticationPrincipal User user) {

    EnvironmentResponse environment = environmentService.startEnvironment(id, user);
    return ResponseEntity.ok(ApiResponse.success(environment, "Environment started"));
  }

  @PostMapping("/{id}/stop")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Stop environment")
  public ResponseEntity<ApiResponse<EnvironmentResponse>> stopEnvironment(
      @PathVariable Long id,
      @AuthenticationPrincipal User user) {

    EnvironmentResponse environment = environmentService.stopEnvironment(id, user);
    return ResponseEntity.ok(ApiResponse.success(environment, "Environment stopped"));
  }

  @PostMapping("/{id}/restart")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Restart environment")
  public ResponseEntity<ApiResponse<EnvironmentResponse>> restartEnvironment(
      @PathVariable Long id,
      @AuthenticationPrincipal User user) {

    EnvironmentResponse environment = environmentService.restartEnvironment(id, user);
    return ResponseEntity.ok(ApiResponse.success(environment, "Environment restarted"));
  }

  @GetMapping("/{id}/logs")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Get environment logs")
  public ResponseEntity<ApiResponse<String>> getEnvironmentLogs(
      @PathVariable Long id,
      @RequestParam(defaultValue = "100") int tailLines) {

    String logs = environmentService.getEnvironmentLogs(id, tailLines);
    return ResponseEntity.ok(ApiResponse.success(logs));
  }

  @GetMapping("/{id}/history")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Get environment audit history")
  public ResponseEntity<ApiResponse<?>> getEnvironmentHistory(@PathVariable Long id) {
    var history = environmentService.getEnvironmentHistory(id);
    return ResponseEntity.ok(ApiResponse.success(history));
  }

  // Deployment endpoints within environment context
  @PostMapping("/{id}/deploy")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Trigger new deployment")
  public ResponseEntity<ApiResponse<DeploymentResponse>> triggerDeployment(
      @PathVariable Long id,
      @RequestBody(required = false) DeployRequest request,
      @AuthenticationPrincipal User user) {

    if (request == null) {
      request = new DeployRequest();
    }
    DeploymentResponse deployment = deploymentService.triggerDeployment(id, request, user);
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(ApiResponse.success(deployment, "Deployment triggered"));
  }

  @GetMapping("/{id}/deployments")
  @PreAuthorize("hasRole('DEVELOPER') or hasRole('PLATFORM_ADMIN')")
  @Operation(summary = "Get deployments for environment")
  public ResponseEntity<ApiResponse<List<DeploymentResponse>>> getDeployments(@PathVariable Long id) {
    List<DeploymentResponse> deployments = deploymentService.getDeploymentsByEnvironment(id);
    return ResponseEntity.ok(ApiResponse.success(deployments));
  }
}
