# Lemfi QA Homework — Backend

This is the **backend part** of the QA homework assignment.

The application is a small **Spring Boot** API using an in-memory **H2 database**, documented via **Swagger (OpenAPI)**.

---

## 🚀 How to Run

### 🔹 Option 1 — Using Docker (recommended)
```bash
docker compose up --build
```

Once the container is built and started, the app will be available at:
👉 http://localhost:8080

To stop the container:

```
docker compose down
```


### 🔹 Option 2 — Run locally via IDE or Gradle

- Open the project in your IDE (e.g., IntelliJ IDEA).

- Locate the main class:
src/main/java/.../HomeworkApplication.java
- Run it as a standard Spring Boot application (Run → HomeworkApplication).

### 📚 API Documentation (Swagger)

Once the application is running, you can explore the API using Swagger UI:

Type	URL
Swagger UI	http://localhost:8080/swagger-ui/index.html

OpenAPI JSON	http://localhost:8080/v3/api-docs


### 🧩 Tech Stack

- Java 17 / 21

- Spring Boot

- Spring Data JPA

- Spring Security

- H2 Database

- Swagger (springdoc-openapi)

- Docker / Docker Compose