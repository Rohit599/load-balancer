package com.liftlab.loadbalancer.algorithm;

import com.liftlab.loadbalancer.config.ServerConfig.Server;

import java.util.List;

public interface LoadBalancingAlgorithm {
  Server selectServer(List<Server> servers);
  String getAlgorithmName();
}
