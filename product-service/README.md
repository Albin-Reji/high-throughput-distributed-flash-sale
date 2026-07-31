# Product Service 📦

The **Product Service** is a Spring Boot microservice responsible for handling product entity operations, data persistence, and REST endpoints for **Project Aegis**.

---

## 🛠️ Tech Stack & Dependencies

- **Java Version**: 21
- **Framework**: Spring Boot 4.x / 3.4+
- **Database**: PostgreSQL
- **Persistence**: Spring Data JPA / Hibernate
- **Utilities**: Lombok, Spring Boot DevTools
- **Monitoring**: Spring Boot Actuator

---

## 📂 Project Structure

```
product-service/
├── src/
│   ├── main/
│   │   ├── java/com/aegis/product_service/
│   │   │   └── ProductServiceApplication.java
│   │   └── resources/
│   │       └── application.yaml
│   └── test/
│       └── java/com/aegis/product_service/
│           └── ProductServiceApplicationTests.java
├── mvnw & mvnw.cmd
└── pom.xml
```

---

## 🚀 Running the Service

### Prerequisites
- JDK 21+ installed and configured in `JAVA_HOME`.
- Active PostgreSQL database instance.

### Step-by-Step Setup

1. **Configure Application Properties**
   Edit `src/main/resources/application.yaml` to specify your PostgreSQL connection settings:
   ```yaml
   spring:
     application:
       name: product-service
     datasource:
       url: jdbc:postgresql://localhost:5432/product_db
       username: postgres
       password: secret_password
     jpa:
       hibernate:
         ddl-auto: update
   ```

2. **Compile and Package**
   ```bash
   .\mvnw.cmd clean package
   ```

3. **Run Application**
   ```bash
   .\mvnw.cmd spring-boot:run
   ```

---

## 🧪 Running Tests

Execute automated unit and integration tests using Maven:

```bash
.\mvnw.cmd test
```

---

## 📊 Endpoints & Monitoring

- **Actuator Health**: `http://localhost:8080/actuator/health`
- **Actuator Metrics**: `http://localhost:8080/actuator/metrics`
