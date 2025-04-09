package com.liftlab.loadbalancer.scheduler;

import com.liftlab.loadbalancer.config.ServerConfig;
import com.liftlab.loadbalancer.service.HealthCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HealthCheckSchedulerTest {

  @Mock
  private HealthCheckService healthCheckService;

  @Mock
  private ServerConfig serverConfig;

  @InjectMocks
  private HealthCheckScheduler healthCheckScheduler;

  @BeforeEach
  void setUp() {
    // Reset mocks before each test
    reset(healthCheckService);
  }

  @Test
  @DisplayName("GIVEN scheduler runs WHEN scheduleHealthChecks is called THEN it should invoke health check service once")
  void scheduleHealthChecks_ShouldCallHealthCheckService() {
    healthCheckScheduler.scheduleHealthChecks();

    verify(healthCheckService, times(1)).checkServersHealth();
  }

  @Test
  @DisplayName("GIVEN health check service throws error WHEN scheduleHealthChecks is called THEN it should catch and log the exception")
  void scheduleHealthChecks_WhenHealthCheckServiceThrowsException_ShouldLogError() {
    doThrow(new RuntimeException("Test error")).when(healthCheckService).checkServersHealth();

    healthCheckScheduler.scheduleHealthChecks();

    verify(healthCheckService, times(1)).checkServersHealth();
  }

  @Test
  @DisplayName("GIVEN fixed schedule WHEN scheduleHealthChecks is triggered THEN it should call health check service at fixed rate")
  void scheduleHealthChecks_ShouldBeCalledAtFixedRate() {

    healthCheckScheduler.scheduleHealthChecks();

    verify(healthCheckService, times(1)).checkServersHealth();
  }

  @Test
  @DisplayName("GIVEN multiple scheduler invocations WHEN scheduleHealthChecks is called multiple times THEN it should invoke health check service each time")
  void scheduleHealthChecks_WithMultipleCalls_ShouldCallHealthCheckServiceEachTime() {
    healthCheckScheduler.scheduleHealthChecks();
    healthCheckScheduler.scheduleHealthChecks();
    healthCheckScheduler.scheduleHealthChecks();

    verify(healthCheckService, times(3)).checkServersHealth();
  }

}