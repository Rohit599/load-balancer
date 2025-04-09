package com.liftlab.loadbalancer.service;

import com.liftlab.loadbalancer.config.ServerConfig;
import com.liftlab.loadbalancer.config.ServerConfig.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.config.RequestConfig;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class HealthCheckService {
  private final ServerConfig serverConfig;
  private final LoadBalancerService loadBalancerService;
  private final CloseableHttpClient httpClient;

  public HealthCheckService(ServerConfig serverConfig, LoadBalancerService loadBalancerService) {
    this.serverConfig = serverConfig;
    this.loadBalancerService = loadBalancerService;

    // Configure HTTP client with timeout
    RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(serverConfig.getHealthCheck().getTimeoutSeconds(), TimeUnit.SECONDS)
            .setResponseTimeout(serverConfig.getHealthCheck().getTimeoutSeconds(), TimeUnit.SECONDS)
            .build();

    this.httpClient = HttpClients.custom()
            .setDefaultRequestConfig(config)
            .build();
  }

  public void checkServersHealth() {
    log.debug("Starting health check for all servers");
    serverConfig.getServers().forEach(this::checkServerHealth);
  }

  private void checkServerHealth(Server server) {
    try {
      String healthCheckUrl = server.getUrl() + serverConfig.getHealthCheck().getPath();
      HttpGet request = new HttpGet(healthCheckUrl);

      boolean isHealthy = httpClient.execute(request, response -> {
        int statusCode = response.getCode();
        return statusCode >= 200 && statusCode < 300;
      });

      updateServerHealth(server, isHealthy);
    } catch (IOException e) {
      log.error("Health check failed for server {}: {}", server.getUrl(), e.getMessage());
      updateServerHealth(server, false);
    }
  }

  private void updateServerHealth(Server server, boolean isHealthy) {
    if (isHealthy) {
      server.setConsecutiveFailures(0);
      if (!server.isHealthy()) {
        // Server needs multiple successful checks to be marked healthy
        if (server.getConsecutiveFailures() >= serverConfig.getHealthCheck().getSuccessThreshold()) {
          log.info("Server {} is now healthy after {} successful checks",
                  server.getUrl(), serverConfig.getHealthCheck().getSuccessThreshold());
          loadBalancerService.markServerHealthy(server.getUrl());
        }
      }
    } else {
      server.setConsecutiveFailures(server.getConsecutiveFailures() + 1);
      if (server.isHealthy() && server.getConsecutiveFailures() >= serverConfig.getHealthCheck().getMaxFailures()) {
        log.warn("Server {} is now unhealthy after {} consecutive failures",
                server.getUrl(), serverConfig.getHealthCheck().getMaxFailures());
        loadBalancerService.markServerUnhealthy(server.getUrl());
      }
    }
  }
}