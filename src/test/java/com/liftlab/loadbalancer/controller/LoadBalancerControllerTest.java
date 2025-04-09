package com.liftlab.loadbalancer.controller;

import com.liftlab.loadbalancer.config.ServerConfig.Server;
import com.liftlab.loadbalancer.service.LoadBalancerService;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadBalancerControllerTest {

  @Mock
  private LoadBalancerService loadBalancerService;

  @Mock
  private HttpServletRequest request;

  @InjectMocks
  private LoadBalancerController loadBalancerController;

  @BeforeEach
  void setUp() {
    reset(loadBalancerService, request);
  }

  @Test
  @DisplayName("GIVEN GET request WHEN handleRequest called THEN it should forward successfully")
  void handleRequest_GetRequest_ShouldForwardSuccessfully() throws Exception {
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(loadBalancerService.forwardRequest(any())).thenReturn("Success response");

    ResponseEntity<String> response = loadBalancerController.handleRequest(request);

    assertEquals(200, response.getStatusCode().value());
    assertEquals("Success response", response.getBody());
    verify(loadBalancerService).forwardRequest(any(HttpGet.class));
  }

  @Test
  @DisplayName("GIVEN POST request WHEN handleRequest called THEN it should forward successfully")
  void handleRequest_PostRequest_ShouldForwardSuccessfully() throws Exception {
    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(loadBalancerService.forwardRequest(any())).thenReturn("Success response");

    ResponseEntity<String> response = loadBalancerController.handleRequest(request);

    assertEquals(200, response.getStatusCode().value());
    assertEquals("Success response", response.getBody());
    verify(loadBalancerService).forwardRequest(any(HttpPost.class));
  }

  @Test
  @DisplayName("GIVEN service throws exception WHEN handleRequest called THEN it should return HTTP 500")
  void handleRequest_Error_ShouldReturn500() throws Exception {
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(loadBalancerService.forwardRequest(any())).thenThrow(new RuntimeException("Test error"));

    ResponseEntity<String> response = loadBalancerController.handleRequest(request);

    assertEquals(500, response.getStatusCode().value());
    assertTrue(response.getBody().contains("Error processing request"));
  }

  @Test
  @DisplayName("GIVEN valid algorithm name WHEN setAlgorithm called THEN it should return success response")
  void setAlgorithm_ValidAlgorithm_ShouldReturnSuccess() {
    String algorithmName = "round-robin";

    ResponseEntity<String> response = loadBalancerController.setAlgorithm(algorithmName);

    assertEquals(200, response.getStatusCode().value());
    assertEquals("Algorithm set to: " + algorithmName, response.getBody());
    verify(loadBalancerService).setAlgorithm(algorithmName);
  }

  @Test
  @DisplayName("GIVEN invalid algorithm name WHEN setAlgorithm called THEN it should return HTTP 400")
  void setAlgorithm_InvalidAlgorithm_ShouldReturnBadRequest() {
    String algorithmName = "invalid-algorithm";
    doThrow(new IllegalArgumentException("Invalid algorithm")).when(loadBalancerService).setAlgorithm(algorithmName);

    ResponseEntity<String> response = loadBalancerController.setAlgorithm(algorithmName);

    assertEquals(400, response.getStatusCode().value());
    assertTrue(response.getBody().contains("Invalid algorithm"));
  }

  @Test
  @DisplayName("GIVEN servers are registered WHEN listServers called THEN it should return server list")
  void listServers_ShouldReturnServerList() {
    Server server1 = new Server();
    server1.setUrl("http://server1");
    server1.setHealthy(true);
    server1.setConsecutiveFailures(0);

    Server server2 = new Server();
    server2.setUrl("http://server2");
    server2.setHealthy(true);
    server2.setConsecutiveFailures(0);

    List<Server> servers = Arrays.asList(server1, server2);
    when(loadBalancerService.getRegisteredServers()).thenReturn(servers);

    ResponseEntity<List<Server>> response = loadBalancerController.listServers();

    assertEquals(200, response.getStatusCode().value());
    assertEquals(2, response.getBody().size());
    assertEquals(servers, response.getBody());
  }

  @Test
  @DisplayName("GIVEN server URL WHEN markServerUnhealthy called THEN it should mark server as unhealthy")
  void markServerUnhealthy_ShouldReturnSuccess() {
    String serverUrl = "http://server1";

    ResponseEntity<String> response = loadBalancerController.markServerUnhealthy(serverUrl);

    assertEquals(200, response.getStatusCode().value());
    assertEquals("Server marked as unhealthy: " + serverUrl, response.getBody());
    verify(loadBalancerService).markServerUnhealthy(serverUrl);
  }

  @Test
  @DisplayName("GIVEN server URL WHEN markServerHealthy called THEN it should mark server as healthy")
  void markServerHealthy_ShouldReturnSuccess() {
    String serverUrl = "http://server1";

    ResponseEntity<String> response = loadBalancerController.markServerHealthy(serverUrl);

    assertEquals(200, response.getStatusCode().value());
    assertEquals("Server marked as healthy: " + serverUrl, response.getBody());
    verify(loadBalancerService).markServerHealthy(serverUrl);
  }
}