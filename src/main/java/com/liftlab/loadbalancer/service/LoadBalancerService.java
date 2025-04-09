package com.liftlab.loadbalancer.service;

import com.liftlab.loadbalancer.algorithm.LoadBalancingAlgorithm;
import com.liftlab.loadbalancer.config.ServerConfig;
import com.liftlab.loadbalancer.config.ServerConfig.Server;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoadBalancerService {
  private final ServerConfig serverConfig;
  private final Map<String, LoadBalancingAlgorithm> algorithms;
  @Getter
  private LoadBalancingAlgorithm currentAlgorithm;

  @PostConstruct
  public void init() {
    log.info(algorithms.values().toString());
    currentAlgorithm = algorithms.get(serverConfig.getAlgorithm());
    if (currentAlgorithm == null) {
      throw new IllegalStateException("Invalid load balancing algorithm: " + serverConfig.getAlgorithm());
    }
  }

  public void setAlgorithm(String algorithmName) {
    LoadBalancingAlgorithm algorithm = algorithms.get(algorithmName);
    if (algorithm == null) {
      throw new IllegalArgumentException("Invalid algorithm: " + algorithmName);
    }
    currentAlgorithm = algorithm;
  }

  public String forwardRequest(HttpUriRequest request) {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      Server server = selectServer();
      String targetUrl = server.getUrl() + request.getUri().getPath();

      request.setUri(java.net.URI.create(targetUrl));

      try (ClassicHttpResponse response = httpClient.execute(request)) {
        String responseBody = EntityUtils.toString(response.getEntity());
        log.debug("Response from server {}: {}", server.getUrl(), responseBody);
        return responseBody;
      }
    } catch (Exception e) {
      log.error("Error forwarding request: {}", e.getMessage());
      throw new RuntimeException("Failed to forward request", e);
    }
  }

  public List<Server> getRegisteredServers() {
    return serverConfig.getServers();
  }

  public void markServerUnhealthy(String serverUrl) {
    serverConfig.getServers().stream()
            .filter(server -> server.getUrl().equals(serverUrl))
            .findFirst()
            .ifPresent(server -> server.setHealthy(false));
  }

  public void markServerHealthy(String serverUrl) {
    serverConfig.getServers().stream()
            .filter(server -> server.getUrl().equals(serverUrl))
            .findFirst()
            .ifPresent(server -> server.setHealthy(true));
  }

  private Server selectServer() {
    return currentAlgorithm.selectServer(serverConfig.getServers());
  }

}
