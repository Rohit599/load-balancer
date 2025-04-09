package com.liftlab.loadbalancer.algorithm;

import com.liftlab.loadbalancer.config.ServerConfig.Server;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component("round-robin")
public class RoundRobinAlgorithm implements LoadBalancingAlgorithm {
  private final AtomicInteger currentIndex = new AtomicInteger(0);

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

    int index = currentIndex.getAndIncrement() % healthyServers.size();
    return healthyServers.get(index);
  }

  @Override
  public String getAlgorithmName() {
    return "round-robin";
  }
}
