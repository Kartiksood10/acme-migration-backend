# API Contract Migration Engine (ACME) Control Plane Backend

**The Spring Boot control plane that orchestrates AI-driven API contract migrations and manages asynchronous worker execution.**

The ACME Backend acts as the central nervous system for the ACME ecosystem. While the [ACME Migration Worker](https://github.com/Kartiksood10/acme-migration-worker) handles the heavy lifting of LangGraph orchestration and LLM-powered code generation, this Java Spring Boot service manages migration requests, tracks asynchronous job states, persists results to PostgreSQL, and provides a real-time polling interface for downstream clients.

Instead of blocking HTTP connections during a 5-10 minute AI execution, this backend implements a highly responsive event loop: acknowledging requests instantly, delegating processing to the Python worker, and catching webhook responses.

## 🚀 Key Features

- **Non-Blocking Asynchronous Trigger:** Accepts complex `multipart/form-data` (combining URLs and file uploads) and immediately returns a `202 Accepted` to unblock the caller.
- **Worker Delegation:** Utilizes Spring's modern `RestClient` to securely package and forward source/target API contracts and repository details to the FastAPI Python worker.
- **Webhook Integration:** Exposes a dedicated `POST` webhook endpoint to capture terminal statuses, generated Pull Request URLs, and detailed endpoint logs pushed by the Python worker.
- **Real-Time Polling API:** Features a highly optimized `GET` endpoint utilizing `@Transactional(readOnly = true)` to serve live migration statuses to clients every few seconds without performance degradation.
- **Dashboard Summary API:** Provides a lightweight, paginated master index of historical runs, preventing the fetching of heavy payload logs when simply retrieving a top-level list.
- **Global Exception Handling:** Uses `@RestControllerAdvice` to intercept backend errors (e.g., `JobNotFoundException`) and translate them into standardized, clean JSON responses.
- **Entity-DTO Mapping:** Safely translates heavy Hibernate entities into lightweight DTOs to prevent `LazyInitializationException` and infinite recursion during Jackson serialization.

## 🏗️ Architecture & Worker Linkage

The ACME architecture physically separates fast API serving from slow AI processing.

### 1. ACME Backend
The traffic cop and database manager. It ensures the database accurately reflects the migration's current state (`PROCESSING`, `COMPLETED`, `FAILED`, `PARTIAL_FAILURE`) and serves this state to querying clients.

### 2. ACME Worker (FastAPI / LangGraph)
The autonomous engine. Triggered by this Spring Boot backend, it spins up Docker sandboxes, generates code, raises PRs, and reports back via the webhook. You can find the worker repository here: [https://github.com/Kartiksood10/acme-migration-worker](https://github.com/Kartiksood10/acme-migration-worker)

## 🔄 End-to-End Data Flow

1. **Trigger:** A client sends a `POST /api/v1/migrate` request containing API contracts.
2. **Accept:** Spring Boot saves initial state to PostgreSQL and returns a `202 Accepted`.
3. **Delegate:** Spring Boot forwards the request payload to the Python Worker's `/migrations` endpoint.
4. **Poll:** The client begins polling `GET /api/v1/migrations/{jobId}`. Spring Boot returns `"PROCESSING"`.
5. **Execute:** The Python worker runs its LangGraph pipeline for 5+ minutes.
6. **Callback:** The Python worker finishes and sends a `POST` payload to `api/v1/webhook/migrations`.
7. **Finalize:** Spring Boot updates PostgreSQL with PR links and logs. On the next client poll, it returns `"COMPLETED"`.

## 💻 Tech Stack

| Component                  | Technology                    |
| -------------------------- | ----------------------------- |
| Language                   | Java 21                       |
| Framework                  | Spring Boot 3.x               |
| Web & HTTP                 | Spring Web, RestClient        |
| Database                   | PostgreSQL                    |
| ORM                        | Hibernate, Spring Data JPA    |
| Boilerplate Reduction      | Lombok                        |
| API Design                 | RESTful, Webhook Callback     |

## 🔌 API Usage

Base URL: `http://localhost:8081/api/v1`

### 1. Trigger Migration
`POST /migrate`
Accepts `multipart/form-data` with repository URLs and OpenAPI files. Returns immediate `202 Accepted` with a `jobId`.

### 2. View Dashboard Summary
`GET /migrations`
Returns a lightweight array of all historical `MigrationJobs` (Job ID, Repo URL, Status, Execution Time).

### 3. Poll Migration Details
`GET /migrations/{jobId}`
The short-polling API. Returns deep details including `generatedPrs` and `endpointLogs`.

### 4. Webhook Receiver (Internal Use)
`POST /webhook/migrations`
Strictly for the Python worker to push terminal results. Accepts `WebhookCallbackPayload` JSON.

## 🛠️ Local Setup

### Prerequisites
- Java 21 (JDK)
- Maven 3.8+
- PostgreSQL 14+ running locally
- [ACME Migration Worker](https://github.com/Kartiksood10/acme-migration-worker) running on `localhost:8000`

### 1. Clone the repository
```bash
git clone https://github.com/Kartiksood10/acme-migration-backend.git
cd acme-migration-backend
```

### 2. Configure Database and Endpoints
Update your `src/main/resources/application.properties` with the following configurations:
```properties
server.port=8081

spring.datasource.url=jdbc:postgresql://localhost:5432/acme_db
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update

# Worker Communication
fastapi.endpoint=http://localhost:8000/migrations

# Webhook Callbacks
webhook.callbackUrl=http://localhost:8081/api/v1/webhook/migrations
webhook.test.callbackUrl=https://webhook.site/04518c1e-f23e-4a73-9afc-0fa4afb8958e
```

### 3. Build and Run
```bash
mvn clean install
mvn spring-boot:run
```
The server will start on port `8081`.

## 🎯 Project Highlights

- **Architectural Separation of Concerns:** By using a Webhook/Polling pattern, the system completely avoids hanging HTTP connections and HTTP timeout errors common in AI applications.
- **Production-Ready Exception Handling:** The backend securely intercepts exceptions, guaranteeing predictable JSON API responses.
- **Memory Optimized:** Utilizing `@Transactional(readOnly = true)` bypasses Hibernate's dirty-checking overhead for the high-frequency polling API.
