package com.liftlab.loadbalancer.scheduler;

import com.liftlab.loadbalancer.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class HealthCheckScheduler {
  private final HealthCheckService healthCheckService;

  @Scheduled(fixedRateString = "#{@serverConfig.healthCheck.intervalSeconds}", timeUnit = TimeUnit.SECONDS)
  public void scheduleHealthChecks() {
    log.debug("Scheduled health check triggered");
    healthCheckService.checkServersHealth();
  }
}