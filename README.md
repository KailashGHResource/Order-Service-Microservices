# Order Service Microservices Architecture

A distributed microservices architecture built with Spring Boot 3.3, Spring Cloud, and Java 21.

## 🏗️ Architecture Overview
Client Request ➔ API Gateway (:8080) ➔ Eureka Service Discovery (:8761) ➔ Order Service (:8086)

### Included Services
* **Service Registry (`service-registry`):** Netflix Eureka server for dynamic service registration and discovery (Port: 8761).
* **API Gateway (`api-gateway`):** Central entry point featuring Spring Cloud Gateway, dynamic routing via `lb://`, distributed correlation ID tracing filter, and Resilience4j circuit breaker fallbacks (Port: 8080).
* **Order Service (`order-service`):** Core business microservice handling order management, persistence with PostgreSQL, distributed caching/locking with Redis, and Kafka event streaming (Port: 8086).

## 🚀 Key Features
* **Dynamic Routing & Load Balancing:** Microservices discoverable via Eureka using `lb://` URIs.
* **Observability & Tracing:** Custom `GlobalFilter` injecting and propagating `X-Correlation-ID`.
* **Fault Tolerance:** Resilience4j Circuit Breaker redirecting unprovisioned downstream calls to localized 503 fallback handlers.

## ⚙️ How to Run Locally
1. Start infrastructure: PostgreSQL (5432), Kafka (9092), Redis (6379).
2. Start **Service Registry** (`service-registry`).
3. Start **Order Service** (`order-service`).
4. Start **API Gateway** (`api-gateway`).
