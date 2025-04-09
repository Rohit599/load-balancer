package com.liftlab.loadbalancer.controller;

import com.liftlab.loadbalancer.config.ServerConfig.Server;
import com.liftlab.loadbalancer.service.LoadBalancerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.methods.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoadBalancerController {
  private final LoadBalancerService loadBalancerService;

  @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
  public ResponseEntity<String> handleRequest(HttpServletRequest request) {
    try {
      HttpUriRequest httpRequest = createHttpRequest(request);
      String response = loadBalancerService.forwardRequest(httpRequest);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(500).body("Error processing request: " + e.getMessage());
    }
  }

  @PostMapping("/algorithm/{algorithmName}")
  public ResponseEntity<String> setAlgorithm(@PathVariable String algorithmName) {
    try {
      loadBalancerService.setAlgorithm(algorithmName);
      return ResponseEntity.ok("Algorithm set to: " + algorithmName);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @GetMapping("/server/list")
  public ResponseEntity<List<Server>> listServers() {
    return ResponseEntity.ok(loadBalancerService.getRegisteredServers());
  }

  @PostMapping("/server/{serverUrl}/unhealthy")
  public ResponseEntity<String> markServerUnhealthy(@PathVariable String serverUrl) {
    loadBalancerService.markServerUnhealthy(serverUrl);
    return ResponseEntity.ok("Server marked as unhealthy: " + serverUrl);
  }

  @PostMapping("/server/{serverUrl}/healthy")
  public ResponseEntity<String> markServerHealthy(@PathVariable String serverUrl) {
    loadBalancerService.markServerHealthy(serverUrl);
    return ResponseEntity.ok("Server marked as healthy: " + serverUrl);
  }

  private HttpUriRequest createHttpRequest(HttpServletRequest request) {
    String method = request.getMethod();
    String uri = request.getRequestURI();

    return switch (method) {
      case "GET" -> new HttpGet(uri);
      case "POST" -> new HttpPost(uri);
      case "PUT" -> new HttpPut(uri);
      case "DELETE" -> new HttpDelete(uri);
      default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
    };
  }
}
