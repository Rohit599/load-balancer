package com.liftlab.loadbalancer.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "loadbalancer")
@Validated
@Slf4j
public class ServerConfig {
  @NotEmpty(message = "At least one server must be configured")
  private List<Server> servers = new ArrayList<>();

  @NotNull(message = "Algorithm must be specified")
  private String algorithm = "round-robin";

  @Data
  public static class Server {
    @NotEmpty(message = "Server URL cannot be empty")
    private String url;
    private boolean healthy = true;
  }

  @PostConstruct
  public void validateServers() {
    if (servers.isEmpty()) {
      throw new IllegalStateException("No servers configured in application.yml");
    }
    log.info("Initialized {} servers with algorithm: {}", servers.size(), algorithm);
  }
}
