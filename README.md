# Enterprise Employee Management Microservices

A highly scalable, event-driven microservices architecture built with **Spring Boot 3.3** and **Java 21**. This project demonstrates enterprise-level patterns including Clean Architecture, Distributed Locking, the Transactional Outbox Pattern, and asynchronous event-driven communication.

## 🏗️ Architecture Overview

The system is composed of several independent microservices communicating seamlessly via REST and RabbitMQ. 

Client Request ➔ API Gateway ➔ Core Services (Employee/Leave/Department) ➔ RabbitMQ ➔ Async Services (Audit/Notification)

### Core Infrastructure
* **API Gateway (`api-gateway`)**: The single entry point for all client requests. Handles routing, load balancing, and rate limiting using Resilience4j.
* **Service Registry (`Eureka.demo`)**: Netflix Eureka server for dynamic service discovery.
* **Config Server (`config`)**: Centralized configuration management for all microservices.

### Business Microservices
* **Employee Service (`employee-service`)**: Manages employee onboarding workflows. Built using Clean Architecture (separating Domain, Application, and Infrastructure layers).
* **Department Service (`department-service`)**: Manages department entities and hierarchies.
* **Leave Service (`leave-service`)**: Handles leave applications, bulk approvals, and conflict resolution using **Optimistic Locking** and **Redis Distributed Locks**.
* **Audit Service (`audit-service`)**: Asynchronously listens to domain events (e.g., `EmployeeCreatedEvent`) via RabbitMQ to maintain a secure audit trail.
* **Notification Service (`notificationservice`)**: Listens to messaging queues to dispatch alerts (e.g., Leave Approved/Rejected).

## 🚀 Key Enterprise Features Implemented

* **Transactional Outbox Pattern:** Ensures 100% reliable message delivery to RabbitMQ by saving events to a database outbox table within the same local transaction as the business entity.
* **API Versioning (V1/V2):** Future-proof REST API design allowing backward compatibility.
* **Dynamic Search & Filtering:** Implemented via Spring Data JPA `Specification` for advanced, dynamic queries (Pagination & Sorting).
* **Distributed Concurrency Control:** 
  * **Pessimistic/Distributed Locks:** Uses `Redisson` (Redis) to prevent concurrent duplicate leave requests for the same employee.
  * **Optimistic Locking:** Uses `@Version` to handle concurrent bulk updates gracefully.
* **Partial Failure Handling (HTTP 207):** The bulk-approval API returns `207 Multi-Status` to report which specific IDs succeeded and which failed during bulk operations.
* **Distributed Tracing:** Integrated with **Micrometer** and **Zipkin** for end-to-end request tracking across all services.

## 🛠️ Technology Stack

* **Language:** Java 21
* **Framework:** Spring Boot 3.3, Spring Cloud (Eureka, Config, Gateway, OpenFeign)
* **Database:** PostgreSQL (via Spring Data JPA & Hibernate)
* **Message Broker:** RabbitMQ
* **Caching & Distributed Locks:** Redis (Redisson)
* **Resilience & Fault Tolerance:** Resilience4j (Rate Limiter, Circuit Breaker)
* **Tracing & Observability:** Micrometer, Zipkin
* **Build Tool:** Maven

## ⚙️ How to Run Locally

### 1. Prerequisites
Ensure you have the following installed and running:
* PostgreSQL (Port `5432`)
* RabbitMQ (Port `5672`)
* Redis (Port `6379`)
* Zipkin (Port `9411`) - *Optional for tracing*

### 2. Bootstrapping Order
To start the system locally, run the services in the following order:
1. `config` (Config Server)
2. `Eureka.demo` (Service Registry)
3. `api-gateway`
4. Business Services (`employee-service`, `department-service`, `leave-service`)
5. Event Consumers (`audit-service`, `notificationservice`)
