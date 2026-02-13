# PayGuard 🛡️

**Payment Processing Platform with AI-Powered Fraud Detection**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 📋 Project Overview

PayGuard is an enterprise-grade microservices-based payment processing platform that demonstrates production-ready backend engineering practices. Built with Spring Boot, it features:

- 🏗️ **Microservices Architecture** - 6 independently deployable services
- 🤖 **AI-Powered Fraud Detection** - Real-time ML-based fraud scoring
- 💳 **Stripe Integration** - Payment processing and webhooks
- 📊 **Event-Driven Design** - Apache Kafka for async communication
- ☁️ **Cloud-Native** - AWS deployment with Kubernetes
- 🧪 **Comprehensive Testing** - Unit, integration, and contract tests
- 🔄 **CI/CD Automation** - GitHub Actions pipeline

## 🏛️ System Architecture
```
Client → API Gateway → [User Service, Payment Service, Fraud Engine, 
                        Notification Service, Reconciliation Service]
                    ↓
                Apache Kafka (Event Bus)
                    ↓
              [PostgreSQL, Redis]
```

## 🛠️ Technology Stack

| Category | Technology |
|----------|-----------|
| Language | Java 17+ |
| Framework | Spring Boot 3.x |
| API Gateway | Spring Cloud Gateway |
| Security | OAuth2 + JWT |
| Payment | Stripe Java SDK |
| ML Serving | ONNX Runtime |
| Messaging | Apache Kafka |
| Database | PostgreSQL 15+ |
| Cache | Redis |
| Containers | Docker |
| Orchestration | Kubernetes (AWS EKS) |
| CI/CD | GitHub Actions |
| Testing | JUnit 5, Mockito, Testcontainers |

## 📦 Microservices

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| API Gateway | 8080 | None | Routing, rate limiting, JWT validation |
| User Service | 8081 | PostgreSQL | Authentication, merchant profiles |
| Payment Service | 8082 | PostgreSQL | Stripe integration, transactions |
| Fraud Engine | 8083 | Redis + PostgreSQL | AI fraud detection, scoring |
| Notification Service | 8084 | PostgreSQL | Email/SMS alerts |
| Reconciliation Service | 8085 | PostgreSQL | Settlement matching |

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker Desktop
- PostgreSQL 15+
- Redis
- Apache Kafka

### Local Development Setup
```bash
# Clone the repository
git clone https://github.com/YOUR-USERNAME/payguard.git
cd payguard

# Start infrastructure with Docker Compose
docker-compose up -d

# Build all services
mvn clean install

# Run a specific service (example: Payment Service)
cd payment-service
mvn spring-boot:run
```

## 📚 Documentation

- [Architecture Design](docs/architecture.md)
- [API Contracts](docs/api-contracts/)
- [Architecture Decision Records](docs/adr/)
- [Fraud Detection Engine](docs/fraud-engine.md)

## 🧪 Testing
```bash
# Run all tests
mvn test

# Run integration tests
mvn verify -P integration-tests

# Run specific service tests
cd payment-service && mvn test
```

## 📊 Project Status

- [x] Repository setup
- [ ] Phase 1: Foundation (Weeks 1-2)
- [ ] Phase 2: Payment Core (Weeks 3-4)
- [ ] Phase 3: Fraud Detection (Weeks 5-7)
- [ ] Phase 4: Reconciliation & Notifications (Weeks 8-9)
- [ ] Phase 5: Deployment & Polish (Weeks 10-12)

## 🤝 Contributing

This project follows conventional commits. All pull requests require:
- 1 approval before merge
- Passing CI checks
- Squash and merge only

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Sandra K. Nakayima**  
Senior Software Engineer  
[LinkedIn](https://linkedin.com/in/sandra-kawombe/) | [Email](mailto:sandrakawombe@gmail.com)

---

**Built with ❤️ as part of backend engineering mastery journey**# PayGuard 🛡️

**Payment Processing Platform with AI-Powered Fraud Detection**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 📋 Project Overview

PayGuard is an enterprise-grade microservices-based payment processing platform that demonstrates production-ready backend engineering practices. Built with Spring Boot, it features:

- 🏗️ **Microservices Architecture** - 6 independently deployable services
- 🤖 **AI-Powered Fraud Detection** - Real-time ML-based fraud scoring
- 💳 **Stripe Integration** - Payment processing and webhooks
- 📊 **Event-Driven Design** - Apache Kafka for async communication
- ☁️ **Cloud-Native** - AWS deployment with Kubernetes
- 🧪 **Comprehensive Testing** - Unit, integration, and contract tests
- 🔄 **CI/CD Automation** - GitHub Actions pipeline

## 🏛️ System Architecture
```
Client → API Gateway → [User Service, Payment Service, Fraud Engine, 
                        Notification Service, Reconciliation Service]
                    ↓
                Apache Kafka (Event Bus)
                    ↓
              [PostgreSQL, Redis]
```

## 🛠️ Technology Stack

| Category | Technology |
|----------|-----------|
| Language | Java 17+ |
| Framework | Spring Boot 3.x |
| API Gateway | Spring Cloud Gateway |
| Security | OAuth2 + JWT |
| Payment | Stripe Java SDK |
| ML Serving | ONNX Runtime |
| Messaging | Apache Kafka |
| Database | PostgreSQL 15+ |
| Cache | Redis |
| Containers | Docker |
| Orchestration | Kubernetes (AWS EKS) |
| CI/CD | GitHub Actions |
| Testing | JUnit 5, Mockito, Testcontainers |

## 📦 Microservices

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| API Gateway | 8080 | None | Routing, rate limiting, JWT validation |
| User Service | 8081 | PostgreSQL | Authentication, merchant profiles |
| Payment Service | 8082 | PostgreSQL | Stripe integration, transactions |
| Fraud Engine | 8083 | Redis + PostgreSQL | AI fraud detection, scoring |
| Notification Service | 8084 | PostgreSQL | Email/SMS alerts |
| Reconciliation Service | 8085 | PostgreSQL | Settlement matching |

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker Desktop
- PostgreSQL 15+
- Redis
- Apache Kafka

### Local Development Setup
```bash
# Clone the repository
git clone https://github.com/YOUR-USERNAME/payguard.git
cd payguard

# Start infrastructure with Docker Compose
docker-compose up -d

# Build all services
mvn clean install

# Run a specific service (example: Payment Service)
cd payment-service
mvn spring-boot:run
```

## 📚 Documentation

- [Architecture Design](docs/architecture.md)
- [API Contracts](docs/api-contracts/)
- [Architecture Decision Records](docs/adr/)
- [Fraud Detection Engine](docs/fraud-engine.md)

## 🧪 Testing
```bash
# Run all tests
mvn test

# Run integration tests
mvn verify -P integration-tests

# Run specific service tests
cd payment-service && mvn test
```

## 📊 Project Status

- [x] Repository setup
- [ ] Phase 1: Foundation (Weeks 1-2)
- [ ] Phase 2: Payment Core (Weeks 3-4)
- [ ] Phase 3: Fraud Detection (Weeks 5-7)
- [ ] Phase 4: Reconciliation & Notifications (Weeks 8-9)
- [ ] Phase 5: Deployment & Polish (Weeks 10-12)

## 🤝 Contributing

This project follows conventional commits. All pull requests require:
- 1 approval before merge
- Passing CI checks
- Squash and merge only

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Sandra K. Nakayima**  
Senior Software Engineer  
[LinkedIn](https://linkedin.com/in/sandra-kawombe/) | [Email](mailto:sandrakawombe@gmail.com)

---

**Built with ❤️ as part of backend engineering mastery journey**