# Load Balancer

A Spring Boot-based load balancer that distributes incoming requests across multiple backend servers using configurable load balancing algorithms.

## Features

- Multiple load balancing algorithms (Round Robin, Random)
- Health checking for backend servers
- Configurable server pools
- REST API for management
- Real-time server health monitoring
- Automatic failover

## Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- Spring Boot 3.x
- Apache HttpClient 5.x

## Setup Instructions

1. Clone the repository:
```bash
git clone https://github.com/Rohit599/load-balancer
cd load-balancer
```

2. Configure the application:
    - Update the configuration with your server details and preferences

3. Start mock backend servers using the script `src/main/resources/server.py`
```bash
# Terminal 1 - Start first mock server
python server.py 8081

# Terminal 2 - Start second mock server
python server.py 8082

# Terminal 3 - Start third mock server
python server.py 8083
```

4. Build the project:
```bash
mvn clean install
```

5. Run the application:
```bash
mvn spring-boot:run
```

## Configuration

The load balancer can be configured through `application.yml`:

```yaml
loadbalancer:
  algorithm: round-robin  # or least-connections
  servers:
    - url: http://server1:8080
      healthy: true
    - url: http://server2:8080
      healthy: true
  health-check:
    interval-seconds: 30
    timeout-seconds: 5
    path: /health
    max-failures: 3
    success-threshold: 2
```

## API Endpoints

- `GET /api/**` - Forward requests to backend servers
- `POST /api/algorithm/{algorithmName}` - Change load balancing algorithm
- `GET /api/server/list` - List all registered servers
- `POST /api/server/{serverUrl}/unhealthy` - Mark server as unhealthy
- `POST /api/server/{serverUrl}/healthy` - Mark server as healthy

## Load Balancing Algorithms

1. **Round Robin**
    - Distributes requests evenly across all healthy servers
    - Simple and predictable
    - Good for uniform server capacities

2. **Least Connections**
    - Routes requests to the server with the fewest active connections
    - Better for varying server capacities
    - More complex but better resource utilization

## Health Checking

- Periodic health checks on all servers
- Configurable check interval and timeout
- Automatic server removal on repeated failures
- Manual server health management through API
- Success threshold for server recovery

## Monitoring

- Logging of all load balancing decisions
- Health check results
- Server status changes
- Request forwarding statistics

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details. 