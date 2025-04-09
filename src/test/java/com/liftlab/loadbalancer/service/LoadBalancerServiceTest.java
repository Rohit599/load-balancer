package com.liftlab.loadbalancer.service;

import com.liftlab.loadbalancer.algorithm.LoadBalancingAlgorithm;
import com.liftlab.loadbalancer.config.ServerConfig;
import com.liftlab.loadbalancer.config.ServerConfig.Server;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoadBalancerServiceTest {

  @Mock private ServerConfig serverConfig;
  @Mock private LoadBalancingAlgorithm roundRobinAlgorithm;
  @Mock private LoadBalancingAlgorithm leastConnectionsAlgorithm;
  @Mock private CloseableHttpClient httpClient;
  @Mock private CloseableHttpResponse httpResponse;

  @InjectMocks
  private LoadBalancerService loadBalancerService;

  private Map<String, LoadBalancingAlgorithm> algorithms;
  private List<Server> mockServers;

  @BeforeEach
  void setUp() {
    Server server1 = new Server();
    server1.setUrl("http://server1");
    server1.setHealthy(true);
    server1.setConsecutiveFailures(0);

    Server server2 = new Server();
    server2.setUrl("http://server2");
    server2.setHealthy(true);
    server2.setConsecutiveFailures(0);

    mockServers = Arrays.asList(server1, server2);

    algorithms = new HashMap<>();
    algorithms.put("round-robin", roundRobinAlgorithm);
    algorithms.put("least-connections", leastConnectionsAlgorithm);

    loadBalancerService = new LoadBalancerService(serverConfig, algorithms);

    when(serverConfig.getAlgorithm()).thenReturn("round-robin");
    when(serverConfig.getServers()).thenReturn(mockServers);
    when(roundRobinAlgorithm.selectServer(any())).thenReturn(server1);
  }

  @Test
  @DisplayName("GIVEN valid algorithm in config WHEN init is called THEN it should set the correct algorithm")
  void init_WithValidAlgorithm_ShouldSetCurrentAlgorithm() {
    loadBalancerService.init();
    assertEquals(roundRobinAlgorithm, loadBalancerService.getCurrentAlgorithm());
  }

  @Test
  @DisplayName("GIVEN invalid algorithm in config WHEN init is called THEN it should throw IllegalStateException")
  void init_WithInvalidAlgorithm_ShouldThrowException() {
    when(serverConfig.getAlgorithm()).thenReturn("invalid-algorithm");
    assertThrows(IllegalStateException.class, () -> loadBalancerService.init());
  }

  @Test
  @DisplayName("GIVEN valid algorithm name WHEN setAlgorithm is called THEN it should update the algorithm")
  void setAlgorithm_WithValidAlgorithm_ShouldUpdateCurrentAlgorithm() {
    loadBalancerService.init();
    loadBalancerService.setAlgorithm("least-connections");
    assertEquals(leastConnectionsAlgorithm, loadBalancerService.getCurrentAlgorithm());
  }

  @Test
  @DisplayName("GIVEN invalid algorithm name WHEN setAlgorithm is called THEN it should throw IllegalArgumentException")
  void setAlgorithm_WithInvalidAlgorithm_ShouldThrowException() {
    assertThrows(IllegalArgumentException.class,
            () -> loadBalancerService.setAlgorithm("invalid-algorithm"));
  }

  @Test
  @DisplayName("GIVEN healthy server and valid request WHEN forwardRequest is called THEN it should return expected response")
  void forwardRequest_ShouldForwardToSelectedServer() throws Exception {
    loadBalancerService.init();
    HttpUriRequest request = new HttpGet("/test");
    String expectedResponse = "Success response";

    try (MockedStatic<HttpClients> httpClientsMock = mockStatic(HttpClients.class)) {
      httpClientsMock.when(HttpClients::createDefault).thenReturn(httpClient);
      when(httpClient.execute(any())).thenReturn(httpResponse);
      when(httpResponse.getEntity()).thenReturn(new StringEntity(expectedResponse));

      String response = loadBalancerService.forwardRequest(request);

      assertEquals(expectedResponse, response);
      verify(httpClient).execute(any());
    }
  }

  @Test
  @DisplayName("GIVEN http client throws exception WHEN forwardRequest is called THEN it should throw RuntimeException")
  void forwardRequest_WhenHttpClientFails_ShouldThrowException() throws Exception {
    loadBalancerService.init();
    HttpUriRequest request = new HttpGet("/test");

    try (MockedStatic<HttpClients> httpClientsMock = mockStatic(HttpClients.class)) {
      httpClientsMock.when(HttpClients::createDefault).thenReturn(httpClient);
      when(httpClient.execute(any())).thenThrow(new RuntimeException("Connection failed"));

      assertThrows(RuntimeException.class,
              () -> loadBalancerService.forwardRequest(request));
    }
  }

  @Test
  @DisplayName("GIVEN registered servers WHEN getRegisteredServers is called THEN it should return the server list")
  void getRegisteredServers_ShouldReturnAllServers() {
    List<Server> servers = loadBalancerService.getRegisteredServers();
    assertEquals(mockServers, servers);
    verify(serverConfig).getServers();
  }

  @Test
  @DisplayName("GIVEN server URL WHEN markServerUnhealthy is called THEN it should set server as unhealthy")
  void markServerUnhealthy_ShouldUpdateServerHealthStatus() {
    String serverUrl = "http://server1";

    loadBalancerService.markServerUnhealthy(serverUrl);

    verify(serverConfig).getServers();
    assertFalse(mockServers.getFirst().isHealthy());
  }

  @Test
  @DisplayName("GIVEN unhealthy server URL WHEN markServerHealthy is called THEN it should set server as healthy")
  void markServerHealthy_ShouldUpdateServerHealthStatus() {
    String serverUrl = "http://server1";
    mockServers.getFirst().setHealthy(false);

    loadBalancerService.markServerHealthy(serverUrl);

    verify(serverConfig).getServers();
    assertTrue(mockServers.getFirst().isHealthy());
  }
}
