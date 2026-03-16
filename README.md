# Internal Developer Platform (IDP)

A self-service Internal Developer Platform built with Spring Boot that empowers developers to provision, deploy, observe, and manage their services without deep infrastructure expertise.

## 🎯 Features

### Phase 1: Core API + Docker Integration ✅
- **Environment Management**: Create, update, delete development environments via REST API
- **Docker Integration**: Build images and manage containers using Docker Java SDK
- **Git Integration**: Clone repositories and track commits using JGit

### Phase 2: Observability Integration ✅
- **Metrics**: Micrometer + Prometheus for application and custom metrics
- **Logging**: Centralized logging with Loki + Promtail
- **Visualization**: Pre-configured Grafana dashboards
- **Alerting**: Prometheus alert rules for critical conditions

### Phase 3: Security & Audit ✅
- **Authentication**: JWT-based authentication
- **RBAC**: Role-based access control (DEVELOPER, PLATFORM_ADMIN)
- **Audit Trail**: Spring Data Envers for tracking all changes

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        IDP Platform                              │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐    │
│  │   Auth   │  │  Envs    │  │  Deploy  │  │    Admin     │    │
│  │Controller│  │Controller│  │Controller│  │  Controller  │    │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘    │
│       │             │             │               │             │
│  ┌────┴─────────────┴─────────────┴───────────────┴───────┐    │
│  │                    Service Layer                        │    │
│  │  AuthService | EnvironmentService | DeploymentService   │    │
│  └────┬─────────────┬─────────────────────────────────────┘    │
│       │             │                                           │
│  ┌────┴────┐   ┌────┴────┐   ┌──────────┐                      │
│  │  JPA    │   │ Docker  │   │   Git    │                      │
│  │  Repos  │   │ Service │   │ Service  │                      │
│  └────┬────┘   └────┬────┘   └────┬─────┘                      │
│       │             │             │                             │
└───────┼─────────────┼─────────────┼─────────────────────────────┘
        │             │             │
   ┌────┴────┐   ┌────┴────┐   ┌────┴────┐
   │PostgreSQL│   │ Docker │   │  Git    │
   │  / H2   │   │ Daemon │   │  Repos  │
   └─────────┘   └─────────┘   └─────────┘
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose (for full stack)

### Development Mode (H2 Database)

```bash
# Clone the repository
git clone <your-repo-url>
cd internal-developer-platform

# Run the application
./mvnw spring-boot:run

# Application will be available at http://localhost:8080
```

### Full Stack with Observability

```bash
# Start observability stack (Prometheus, Grafana, Loki)
docker-compose -f docker-compose.dev.yml up -d

# Run the application
./mvnw spring-boot:run

# Access:
# - API: http://localhost:8080
# - Swagger UI: http://localhost:8080/swagger-ui.html
# - Grafana: http://localhost:3000 (admin/admin)
# - Prometheus: http://localhost:9090
```

### Production Mode (PostgreSQL)

```bash
# Start full stack including application
docker-compose up -d

# Or build and run separately
./mvnw clean package -DskipTests
docker build -t idp-app .
docker-compose up -d
```

## 📚 API Documentation

Access the interactive API documentation at: `http://localhost:8080/swagger-ui.html`

### Authentication

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer1",
    "email": "dev1@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer1",
    "password": "password123"
  }'
```

### Environment Management

```bash
# Create environment (use token from login)
curl -X POST http://localhost:8080/api/v1/environments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "serviceName": "user-service",
    "gitRepo": "https://github.com/you/user-svc",
    "branch": "main",
    "description": "User management service"
  }'

# List my environments
curl http://localhost:8080/api/v1/environments/my \
  -H "Authorization: Bearer <your-jwt-token>"

# Get environment logs
curl http://localhost:8080/api/v1/environments/1/logs \
  -H "Authorization: Bearer <your-jwt-token>"

# Get audit history
curl http://localhost:8080/api/v1/environments/1/history \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Deployments

```bash
# Trigger new deployment
curl -X POST http://localhost:8080/api/v1/environments/1/deploy \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "forceRebuild": true
  }'

# Get deployment logs
curl http://localhost:8080/api/v1/deployments/1/logs \
  -H "Authorization: Bearer <your-jwt-token>"
```

## 🔐 Default Users

| Username  | Password   | Roles                     |
|-----------|------------|---------------------------|
| admin     | admin123   | DEVELOPER, PLATFORM_ADMIN |
| developer | dev123     | DEVELOPER                 |

## 📊 Observability

### Metrics
- **Prometheus endpoint**: `http://localhost:8080/actuator/prometheus`
- **Grafana dashboard**: Pre-configured IDP Platform Dashboard

### Custom Metrics
- `idp.environments.total` - Total environments
- `idp.environments.running` - Running environments
- `idp.deployments.success` - Successful deployments counter
- `idp.deployments.failure` - Failed deployments counter
- `idp.deployment.duration` - Deployment duration histogram
- `idp.docker.containers.created` - Containers created
- `idp.docker.builds.success` - Successful image builds

### Logs
- Access via Grafana → Explore → Loki
- Query: `{job="idp"}`

## 🛡️ Security

### Roles & Permissions

| Role           | Permissions                                |
|----------------|--------------------------------------------|
| DEVELOPER      | Manage own environments, view own deploys  |
| PLATFORM_ADMIN | All DEVELOPER + manage all envs + metrics  |

### Protected Endpoints
- `/api/v1/environments/**` - Requires authentication
- `/api/v1/deployments/**` - Requires authentication
- `/api/v1/admin/**` - Requires PLATFORM_ADMIN role

### Public Endpoints
- `/api/v1/auth/**` - Authentication endpoints
- `/actuator/health` - Health check
- `/actuator/prometheus` - Metrics (for Prometheus scraping)
- `/swagger-ui/**` - API documentation

## 🗂️ Project Structure

```
src/main/java/com/idp/
├── IdpApplication.java          # Main application
├── config/                      # Configuration classes
│   ├── SecurityConfig.java      # Security configuration
│   ├── AuditConfig.java         # Audit configuration
│   ├── OpenApiConfig.java       # Swagger/OpenAPI config
│   └── MetricsConfig.java       # Metrics configuration
├── controller/                  # REST controllers
│   ├── AuthController.java
│   ├── EnvironmentController.java
│   ├── DeploymentController.java
│   └── AdminController.java
├── domain/                      # Domain entities
│   ├── entity/
│   │   ├── BaseEntity.java
│   │   ├── Environment.java
│   │   ├── Deployment.java
│   │   ├── User.java
│   │   └── Role.java
│   └── enums/
├── dto/                         # Data transfer objects
│   ├── request/
│   └── response/
├── exception/                   # Exception handling
├── mapper/                      # Entity-DTO mappers
├── repository/                  # JPA repositories
├── security/                    # Security components
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
└── service/                     # Business logic
    ├── AuthService.java
    ├── EnvironmentService.java
    ├── DeploymentService.java
    ├── DockerService.java
    ├── GitService.java
    └── MetricsService.java
```

## 🔧 Configuration

### Environment Variables

| Variable          | Default                        | Description              |
|-------------------|--------------------------------|--------------------------|
| SPRING_PROFILES_ACTIVE | (none)                    | Active profile (prod)    |
| DB_USERNAME       | idp                            | Database username        |
| DB_PASSWORD       | idp                            | Database password        |
| JWT_SECRET        | (default-key)                  | JWT signing key          |
| DOCKER_HOST       | tcp://localhost:2375           | Docker daemon host       |

### Docker Configuration

For Docker-in-Docker or remote Docker daemon:
```yaml
docker:
  host: tcp://localhost:2375  # or tcp://docker-proxy:2375
  registry: ""                # Optional private registry
```

## 📈 Future Enhancements

- [ ] Kubernetes integration with Fabric8 client
- [ ] Service mesh integration (Istio/Linkerd)
- [ ] Cost tracking and resource quotas
- [ ] Slack/Teams notifications
- [ ] GitOps workflow support
- [ ] Multi-tenancy with organization support
- [ ] Custom domain management
- [ ] Secrets management integration (Vault)

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
