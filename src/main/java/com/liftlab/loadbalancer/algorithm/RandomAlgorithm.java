package com.liftlab.loadbalancer.algorithm;

import com.liftlab.loadbalancer.config.ServerConfig.Server;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component("random")
public class RandomAlgorithm implements LoadBalancingAlgorithm {
  private final Random random = new Random();

  @Override
  public Server selectServer(List<Server> servers) {
    if (servers == null || servers.isEmpty()) {
      throw new IllegalStateException("No servers available");
    }

    List<Server> healthyServers = servers.stream()
            .filter(Server::isHealthy)
            .toList();

    if (healthyServers.isEmpty()) {
      throw new IllegalStateException("No healthy servers available");
    }

    int index = random.nextInt(healthyServers.size());
    return healthyServers.get(index);
  }

  @Override
  public String getAlgorithmName() {
    return "random";
  }
}
