package com.idp.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.idp.exception.DockerOperationException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DockerService {

  private DockerClient dockerClient;
  private final MeterRegistry meterRegistry;

  private Counter containerCreatedCounter;
  private Counter containerStartedCounter;
  private Counter containerStoppedCounter;
  private Counter buildSuccessCounter;
  private Counter buildFailureCounter;
  private Timer buildTimer;

  @Value("${docker.host:tcp://localhost:2375}")
  private String dockerHost;

  @Value("${docker.registry:}")
  private String dockerRegistry;

  public DockerService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @PostConstruct
  public void init() {
    try {
      DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
          .withDockerHost(dockerHost)
          .build();

      ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
          .dockerHost(config.getDockerHost())
          .maxConnections(100)
          .connectionTimeout(Duration.ofSeconds(30))
          .responseTimeout(Duration.ofSeconds(45))
          .build();

      dockerClient = DockerClientImpl.getInstance(config, httpClient);
      log.info("Docker client initialized successfully with host: {}", dockerHost);

      // Initialize metrics
      containerCreatedCounter = Counter.builder("idp.docker.containers.created")
          .description("Number of containers created")
          .register(meterRegistry);
      containerStartedCounter = Counter.builder("idp.docker.containers.started")
          .description("Number of containers started")
          .register(meterRegistry);
      containerStoppedCounter = Counter.builder("idp.docker.containers.stopped")
          .description("Number of containers stopped")
          .register(meterRegistry);
      buildSuccessCounter = Counter.builder("idp.docker.builds.success")
          .description("Number of successful image builds")
          .register(meterRegistry);
      buildFailureCounter = Counter.builder("idp.docker.builds.failure")
          .description("Number of failed image builds")
          .register(meterRegistry);
      buildTimer = Timer.builder("idp.docker.build.duration")
          .description("Time taken to build Docker images")
          .register(meterRegistry);

    } catch (Exception e) {
      log.warn("Failed to initialize Docker client: {}. Docker operations will be simulated.", e.getMessage());
    }
  }

  @PreDestroy
  public void cleanup() {
    if (dockerClient != null) {
      try {
        dockerClient.close();
      } catch (IOException e) {
        log.error("Error closing Docker client", e);
      }
    }
  }

  public boolean isAvailable() {
    if (dockerClient == null)
      return false;
    try {
      dockerClient.pingCmd().exec();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String buildImage(File contextDir, String imageName, String tag) {
    if (!isAvailable()) {
      log.warn("Docker not available, simulating image build");
      return "simulated-image-" + UUID.randomUUID().toString().substring(0, 8);
    }

    String fullImageName = (dockerRegistry.isEmpty() ? "" : dockerRegistry + "/") + imageName + ":" + tag;
    log.info("Building Docker image: {} from context: {}", fullImageName, contextDir.getAbsolutePath());

    Timer.Sample sample = Timer.start(meterRegistry);
    try {
      Set<String> tags = new HashSet<>();
      tags.add(fullImageName);

      String imageId = dockerClient.buildImageCmd(contextDir)
          .withTags(tags)
          .withPull(true)
          .withNoCache(false)
          .exec(new BuildImageResultCallback())
          .awaitImageId(10, TimeUnit.MINUTES);

      log.info("Successfully built image: {} with ID: {}", fullImageName, imageId);
      buildSuccessCounter.increment();
      return imageId;

    } catch (Exception e) {
      log.error("Failed to build Docker image: {}", e.getMessage(), e);
      buildFailureCounter.increment();
      throw new DockerOperationException("Failed to build image: " + e.getMessage(), e);
    } finally {
      sample.stop(buildTimer);
    }
  }

  public String createContainer(String imageId, String containerName, int hostPort, int containerPort,
      Map<String, String> envVars, Map<String, String> labels) {
    if (!isAvailable()) {
      log.warn("Docker not available, simulating container creation");
      return "simulated-container-" + UUID.randomUUID().toString().substring(0, 8);
    }

    log.info("Creating container: {} from image: {}", containerName, imageId);

    try {
      // Port bindings
      ExposedPort exposedPort = ExposedPort.tcp(containerPort);
      Ports portBindings = new Ports();
      portBindings.bind(exposedPort, Ports.Binding.bindPort(hostPort));

      // Host configuration
      HostConfig hostConfig = HostConfig.newHostConfig()
          .withPortBindings(portBindings)
          .withRestartPolicy(RestartPolicy.onFailureRestart(3))
          .withMemory(512 * 1024 * 1024L) // 512MB
          .withCpuCount(1L);

      // Environment variables
      List<String> envList = new ArrayList<>();
      if (envVars != null) {
        envVars.forEach((key, value) -> envList.add(key + "=" + value));
      }
      // Add observability endpoints
      envList.add("MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,prometheus,metrics");
      envList.add("MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED=true");

      CreateContainerResponse container = dockerClient.createContainerCmd(imageId)
          .withName(containerName)
          .withExposedPorts(exposedPort)
          .withHostConfig(hostConfig)
          .withEnv(envList)
          .withLabels(labels != null ? labels : new HashMap<>())
          .exec();

      log.info("Container created: {}", container.getId());
      containerCreatedCounter.increment();
      return container.getId();

    } catch (Exception e) {
      log.error("Failed to create container: {}", e.getMessage(), e);
      throw new DockerOperationException("Failed to create container: " + e.getMessage(), e);
    }
  }

  public void startContainer(String containerId) {
    if (!isAvailable()) {
      log.warn("Docker not available, simulating container start");
      return;
    }

    log.info("Starting container: {}", containerId);
    try {
      dockerClient.startContainerCmd(containerId).exec();
      containerStartedCounter.increment();
      log.info("Container started: {}", containerId);
    } catch (Exception e) {
      log.error("Failed to start container: {}", e.getMessage(), e);
      throw new DockerOperationException("Failed to start container: " + e.getMessage(), e);
    }
  }

  public void stopContainer(String containerId) {
    if (!isAvailable()) {
      log.warn("Docker not available, simulating container stop");
      return;
    }

    log.info("Stopping container: {}", containerId);
    try {
      dockerClient.stopContainerCmd(containerId)
          .withTimeout(30)
          .exec();
      containerStoppedCounter.increment();
      log.info("Container stopped: {}", containerId);
    } catch (Exception e) {
      log.error("Failed to stop container: {}", e.getMessage(), e);
      throw new DockerOperationException("Failed to stop container: " + e.getMessage(), e);
    }
  }

  public void removeContainer(String containerId, boolean force) {
    if (!isAvailable()) {
      log.warn("Docker not available, simulating container removal");
      return;
    }

    log.info("Removing container: {}", containerId);
    try {
      dockerClient.removeContainerCmd(containerId)
          .withForce(force)
          .withRemoveVolumes(true)
          .exec();
      log.info("Container removed: {}", containerId);
    } catch (Exception e) {
      log.error("Failed to remove container: {}", e.getMessage(), e);
      throw new DockerOperationException("Failed to remove container: " + e.getMessage(), e);
    }
  }

  public String getContainerLogs(String containerId, int tailLines) {
    if (!isAvailable()) {
      return "Docker not available - logs not accessible";
    }

    try {
      StringBuilder logs = new StringBuilder();
      dockerClient.logContainerCmd(containerId)
          .withStdOut(true)
          .withStdErr(true)
          .withTail(tailLines)
          .withTimestamps(true)
          .exec(new ResultCallback.Adapter<Frame>() {
            @Override
            public void onNext(Frame frame) {
              logs.append(new String(frame.getPayload()));
            }
          }).awaitCompletion(30, TimeUnit.SECONDS);
      return logs.toString();
    } catch (Exception e) {
      log.error("Failed to get container logs: {}", e.getMessage(), e);
      return "Error retrieving logs: " + e.getMessage();
    }
  }

  public InspectContainerResponse.ContainerState getContainerState(String containerId) {
    if (!isAvailable()) {
      return null;
    }

    try {
      return dockerClient.inspectContainerCmd(containerId)
          .exec()
          .getState();
    } catch (Exception e) {
      log.error("Failed to inspect container: {}", e.getMessage(), e);
      return null;
    }
  }

  public void pullImage(String imageName) {
    if (!isAvailable()) {
      log.warn("Docker not available, skipping image pull");
      return;
    }

    log.info("Pulling image: {}", imageName);
    try {
      dockerClient.pullImageCmd(imageName)
          .exec(new PullImageResultCallback())
          .awaitCompletion(10, TimeUnit.MINUTES);
      log.info("Image pulled: {}", imageName);
    } catch (Exception e) {
      log.error("Failed to pull image: {}", e.getMessage(), e);
      throw new DockerOperationException("Failed to pull image: " + e.getMessage(), e);
    }
  }

  public List<Container> listContainers(boolean showAll) {
    if (!isAvailable()) {
      return Collections.emptyList();
    }

    try {
      return dockerClient.listContainersCmd()
          .withShowAll(showAll)
          .exec();
    } catch (Exception e) {
      log.error("Failed to list containers: {}", e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  public void removeImage(String imageId) {
    if (!isAvailable()) {
      return;
    }

    try {
      dockerClient.removeImageCmd(imageId)
          .withForce(true)
          .exec();
      log.info("Image removed: {}", imageId);
    } catch (Exception e) {
      log.error("Failed to remove image: {}", e.getMessage(), e);
    }
  }
}
