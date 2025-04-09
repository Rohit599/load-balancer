package com.liftlab.loadbalancer.service;

import com.liftlab.loadbalancer.config.ServerConfig;
import com.liftlab.loadbalancer.config.ServerConfig.Server;
import com.liftlab.loadbalancer.config.ServerConfig.HealthCheck;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HealthCheckServiceTest {

  @Mock private ServerConfig serverConfig;
  @Mock private HealthCheck healthCheckConfig;
  @Mock private LoadBalancerService loadBalancerService;
  @Mock private CloseableHttpClient httpClient;

  private HealthCheckService healthCheckService;
  private Server testServer;

  @BeforeEach
  void setUp() {
    testServer = new Server();
    testServer.setUrl("http://server1");
    testServer.setHealthy(true);
    testServer.setConsecutiveFailures(0);

    when(healthCheckConfig.getTimeoutSeconds()).thenReturn(5);
    when(healthCheckConfig.getPath()).thenReturn("/health");
    when(healthCheckConfig.getSuccessThreshold()).thenReturn(3);
    when(healthCheckConfig.getMaxFailures()).thenReturn(2);
    when(serverConfig.getHealthCheck()).thenReturn(healthCheckConfig);
    when(serverConfig.getServers()).thenReturn(List.of(testServer));

    healthCheckService = new HealthCheckService(serverConfig, loadBalancerService);
  }


  @Test
  @DisplayName("GIVEN server is unhealthy and response is 200 WHEN checkServersHealth is called THEN server should be marked healthy and failures reset")
  void checkServersHealth_shouldHandleHealthyResponse() throws Exception {
    ClassicHttpResponse mockResponse = mock(ClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(200);

    when(httpClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
            .then(invocation -> {
              HttpClientResponseHandler<Boolean> handler = invocation.getArgument(1);
              return handler.handleResponse(mockResponse);
            });

    testServer.setHealthy(false);
    testServer.setConsecutiveFailures(3);

    healthCheckService.checkServersHealth();

    verify(loadBalancerService).markServerHealthy(testServer.getUrl());
    assertThat(testServer.getConsecutiveFailures()).isEqualTo(0);
  }

  @Test
  @DisplayName("GIVEN server is healthy and response is 500 WHEN checkServersHealth is called THEN server should be marked unhealthy and failures incremented")
  void checkServersHealth_shouldHandleUnhealthyResponse() throws Exception {
    ClassicHttpResponse mockResponse = mock(ClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(500);

    when(httpClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
            .then(invocation -> {
              HttpClientResponseHandler<Boolean> handler = invocation.getArgument(1);
              return handler.handleResponse(mockResponse);
            });

    testServer.setHealthy(true);
    testServer.setConsecutiveFailures(1);

    healthCheckService.checkServersHealth();

    verify(loadBalancerService).markServerUnhealthy(testServer.getUrl());
    assertThat(testServer.getConsecutiveFailures()).isEqualTo(2);
  }

  @Test
  @DisplayName("GIVEN server is healthy and IOException occurs WHEN checkServersHealth is called THEN server should be marked unhealthy and failures incremented")
  void checkServersHealth_shouldHandleIOException() throws Exception {
    when(httpClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
            .thenThrow(new IOException("Connection failed"));

    testServer.setConsecutiveFailures(1);
    testServer.setHealthy(true);

    healthCheckService.checkServersHealth();

    verify(loadBalancerService).markServerUnhealthy(testServer.getUrl());
    assertThat(testServer.getConsecutiveFailures()).isEqualTo(2);
  }
}
