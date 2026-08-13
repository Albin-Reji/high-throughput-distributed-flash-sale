# Project Aegis 🛡️

**Project Aegis** is an enterprise microservices-based application platform built using modern Java and Spring Boot
technologies.

---

## 🏗️ Project Architecture

```
project-aegis/
└── product-service/       # Spring Boot Microservice for Product Management
```

---

## 📦 Services

### 1. Product Service (`/product-service`)

The **Product Service** manages product catalogs, persistence, and REST APIs for domain entities.

- **Tech Stack**: Java 21, Spring Boot 4.x, Spring Data JPA, PostgreSQL, Lombok, Spring Boot Actuator
- **Build Tool**: Apache Maven (`mvnw`)
- **Package Base**: `com.aegis.product_service`

---

## 🚀 Getting Started

### Prerequisites

- **Java**: JDK 21 or higher
- **Database**: PostgreSQL database server
- **Build System**: Maven (wrapper included)

### Building the Project

To build the entire solution or individual services, navigate to the specific service directory:

```bash
# Navigate to Product Service
cd product-service

# Build using Maven wrapper (Windows)
.\mvnw.cmd clean package

# Build using Maven wrapper (Linux/macOS)
./mvnw clean package
```

### Running the Application

```bash
# Run locally with Spring Boot Maven plugin
.\mvnw.cmd spring-boot:run
```

---

## ⚙️ Configuration

Each service manages its configuration inside its `src/main/resources/application.yaml`.

### Database Configuration (PostgreSQL)

Ensure PostgreSQL is running and update database credentials in `product-service/src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: product-service
  datasource:
    url: jdbc:postgresql://localhost:5432/aegis_product_db
    username: postgres
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

---

## 🩺 Monitoring & Health Checks

Spring Boot Actuator is integrated for service health and operational metrics monitoring:

- **Health Endpoint**: `http://localhost:8080/actuator/health`
- **Info Endpoint**: `http://localhost:8080/actuator/info`

---

## 📜 License

This project is proprietary and maintained under **Project Aegis**.
