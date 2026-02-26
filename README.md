# FX Deals Import System 💱

A high-performance, multithreaded REST API built with Spring Boot 3 and Java 21 to handle the ingestion of large datasets (FX Deals) into a PostgreSQL database. Designed to meet Bloomberg-level requirements for scalability, idempotency, and resilience.

## 🚀 Architecture & Key Features

* **Multithreading & Concurrency:** Uses a custom `ExecutorService` with 10 daemon threads to process chunks of data in parallel, maximizing CPU utilization.
* **Batch Processing:** Splits massive payloads into manageable batches (1000 records/chunk) to prevent memory overflow (OOM) and database bottlenecks.
* **Idempotency & Duplicate Handling:** Fetches existing IDs in batches and filters duplicates in-memory *before* attempting database insertion, optimizing DB I/O.
* **No-Rollback Tolerance:** A single failed row (e.g., invalid data or constraint violation) is caught and logged, allowing the rest of the batch to persist successfully.
* **RFC 7807 Error Handling:** Implements standardized error responses using Spring Boot's `ProblemDetail` via a `@RestControllerAdvice`.
* **Jakarta Bean Validation:** Comprehensive validation at the DTO level with ISO 4217 currency code validation, positive amount checks, and required field constraints.
* **Connection Pooling:** Optimized HikariCP configuration with 15 max connections and batch size of 1000 for high throughput.

## 🛠️ Tech Stack

* **Language:** Java 21
* **Framework:** Spring Boot 4.0.3, Spring Data JPA, Hibernate
* **Database:** PostgreSQL 15
* **Containerization:** Docker, Docker Compose (Multi-stage build)
* **Testing:** JUnit 5, Mockito, Testcontainers (Integration Testing), JaCoCo (Coverage)
* **Build Tool:** Maven 3.9.6
* **Utilities:** Lombok (Code generation)

## 📦 Project Structure

```
progressoft-ClusteredData/
├── src/
│   ├── main/
│   │   ├── java/com/progressoft/clusterdata/
│   │   │   ├── controller/          # REST endpoints
│   │   │   │   └── DealController.java
│   │   │   ├── service/             # Business logic & parallel processing
│   │   │   │   └── DealService.java
│   │   │   ├── entity/              # JPA entities
│   │   │   │   └── Deal.java
│   │   │   ├── dto/                 # Request/Response objects
│   │   │   │   ├── DealRequest.java
│   │   │   │   └── DealImportResponse.java
│   │   │   ├── repository/          # Data access layer
│   │   │   │   └── DealRepository.java
│   │   │   ├── controlleradvicer/   # Global exception handling
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── ClusterdataApplication.java
│   │   └── resources/
│   │       └── application.yaml
│   └── test/
│       └── java/com/progressoft/clusterdata/
│           ├── ClusterdataApplicationTests.java
│           ├── integration/
│           │   └── DealIntegrationTest.java
│           └── service/
│               └── DealServiceTest.java
├── docker-compose.yml
├── Dockerfile
├── Makefile
└── pom.xml
```

## ⚙️ How to Run (Local Environment)

This project includes a `Makefile` to simplify building and running the application.

### Prerequisites
* Docker & Docker Compose installed
* Make installed (optional, commands can be run manually)
* Java 21 (if running locally without Docker)

### Quick Start

1. **Start the Database & Application via Docker:**
   ```bash
   make up
   ```
   *(Wait a few seconds for the database to be ready and the app to start on port 8080)*

2. **Check Logs:**
   ```bash
   make logs
   ```

3. **Stop & Clean up:**
   ```bash
   make down
   ```

### Alternative: Manual Docker Commands

```bash
# Start services
docker-compose up -d --build

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

### Running Locally (Without Docker)

1. Start PostgreSQL manually (or use Docker for DB only):
   ```bash
   docker-compose up -d db
   ```

2. Set environment variables:
   ```bash
   export DB_URL=jdbc:postgresql://localhost:5433/bloomberg_fx
   export DB_USERNAME=postgres
   export DB_PASSWORD=postgres
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

## 🧪 Testing

### Run All Tests (Unit + Integration)

```bash
make test
```

This will:
- Execute unit tests with Mockito
- Run integration tests using Testcontainers (spins up a real PostgreSQL instance)
- Generate JaCoCo coverage report in `target/site/jacoco/index.html`

### Generate Coverage Report Only

```bash
make coverage
```

### View Coverage Report

Open `target/site/jacoco/index.html` in your browser after running tests.

## 📡 API Usage

### Endpoint: Import FX Deals

**POST** `/api/v1/deals`

**Content-Type:** `application/json`

### Request Payload Example

```json
[
  {
    "dealUniqueId": "FX-1001",
    "fromCurrency": "USD",
    "toCurrency": "MAD",
    "dealTimestamp": "2026-02-26T10:15:30",
    "dealAmount": 15000.50
  },
  {
    "dealUniqueId": "FX-1002",
    "fromCurrency": "EUR",
    "toCurrency": "MAD",
    "dealTimestamp": "2026-02-26T10:16:00",
    "dealAmount": 8500.00
  }
]
```

### Successful Response (200 OK)

```json
{
  "totalReceived": 2,
  "successfulImports": 2,
  "failedOrSkipped": 0
}
```

### Validation Error Response (400 Bad Request - RFC 7807)

```json
{
  "type": "about:blank",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "instance": "/api/v1/deals",
  "errors": [
    "dealAmount: Deal amount must be positive",
    "fromCurrency: ISO code must be 3 characters"
  ]
}
```

### Malformed JSON Response (400 Bad Request)

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Malformed JSON Request",
  "instance": "/api/v1/deals",
  "error": "JSON parse error: Unexpected character..."
}
```

## 🧩 Key Components

### 1. DealController
- Single endpoint: `POST /api/v1/deals`
- Validates incoming requests using Jakarta Bean Validation
- Delegates processing to `DealService`

### 2. DealService
- **Parallel Processing:** Divides requests into 1000-record chunks
- **CompletableFuture:** Submits chunks to a fixed thread pool (10 threads)
- **Duplicate Detection:** Queries existing deal IDs in batch to filter duplicates before insertion
- **Error Isolation:** Failed records don't affect other records in the batch

### 3. Deal Entity
- Maps to `fx_deals` table in PostgreSQL
- Unique constraint on `deal_unique_id`
- Supports currency codes (3-letter ISO), timestamps, and arbitrary-precision decimals

### 4. GlobalExceptionHandler
- Implements RFC 7807 Problem Details standard
- Handles:
  - `ConstraintViolationException` → 400 with field-level errors
  - `HttpMessageNotReadableException` → 400 for malformed JSON
  - Generic exceptions → 500 Internal Server Error

## 🔧 Configuration

### Database Configuration (`application.yaml`)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 15
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        jdbc:
          batch_size: 1000
```

### Docker Configuration

- **PostgreSQL:** Runs on port `5433` (external) → `5432` (internal)
- **Application:** Runs on port `8080`
- **Health Check:** Ensures database is ready before starting the app
- **Volumes:** Persistent data storage for PostgreSQL

## 📊 Performance Characteristics

| Metric                 | Value                          |
|------------------------|--------------------------------|
| Thread Pool Size       | 10 threads                     |
| Batch Size             | 1000 records/chunk             |
| DB Connection Pool     | 15 max connections             |
| Hibernate Batch Size   | 1000 statements/batch          |
| Processing Strategy    | Parallel (CompletableFuture)   |

## 🧪 Validation Rules

| Field            | Constraints                                    |
|------------------|------------------------------------------------|
| `dealUniqueId`   | Not blank, must be unique                      |
| `fromCurrency`   | Not blank, exactly 3 characters (ISO 4217)     |
| `toCurrency`     | Not blank, exactly 3 characters (ISO 4217)     |
| `dealTimestamp`  | Not null, valid `LocalDateTime`                |
| `dealAmount`     | Not null, must be positive (`BigDecimal`)      |

## 🐳 Docker Commands

### Build and Start

```bash
make up
```

### View Application Logs

```bash
make logs
```

### Access Database CLI

```bash
make db-shell
```

### Check Service Status

```bash
make status
```

### Restart Application

```bash
make restart
```

### Clean Everything (including volumes)

```bash
make clean
```

## 🧮 Testing with cURL

### Import Valid Deals

```bash
curl -X POST http://localhost:8080/api/v1/deals \
  -H "Content-Type: application/json" \
  -d '[
    {
      "dealUniqueId": "FX-1001",
      "fromCurrency": "USD",
      "toCurrency": "MAD",
      "dealTimestamp": "2026-02-26T10:15:30",
      "dealAmount": 15000.50
    },
    {
      "dealUniqueId": "FX-1002",
      "fromCurrency": "EUR",
      "toCurrency": "MAD",
      "dealTimestamp": "2026-02-26T10:16:00",
      "dealAmount": 8500.00
    }
  ]'
```

### Test Duplicate Handling (Re-submit same ID)

```bash
curl -X POST http://localhost:8080/api/v1/deals \
  -H "Content-Type: application/json" \
  -d '[
    {
      "dealUniqueId": "FX-1001",
      "fromCurrency": "GBP",
      "toCurrency": "USD",
      "dealTimestamp": "2026-02-26T11:00:00",
      "dealAmount": 5000.00
    }
  ]'
```

**Expected Response:**
```json
{
  "totalReceived": 1,
  "successfulImports": 0,
  "failedOrSkipped": 1
}
```

### Test Validation Errors

```bash
curl -X POST http://localhost:8080/api/v1/deals \
  -H "Content-Type: application/json" \
  -d '[
    {
      "dealUniqueId": "FX-2000",
      "fromCurrency": "US",
      "toCurrency": "MAD",
      "dealTimestamp": "2026-02-26T10:15:30",
      "dealAmount": -100.00
    }
  ]'
```

**Expected Response:** 400 Bad Request with RFC 7807 Problem Details

## 🧪 Running Tests Locally

### Execute All Tests

```bash
./mvnw clean test
```

### Run Specific Test Class

```bash
./mvnw test -Dtest=DealServiceTest
```

### Run Integration Tests Only

```bash
./mvnw test -Dtest=*IntegrationTest
```

### Generate & View Coverage Report

```bash
make coverage
# Open target/site/jacoco/index.html in browser
```

## 🏗️ Design Decisions

### Why Daemon Threads?

Daemon threads are used in the `ExecutorService` to ensure the JVM can shut down gracefully without waiting for worker threads to complete. This is ideal for background processing tasks.

### Why CompletableFuture?

`CompletableFuture` allows non-blocking, asynchronous processing while maintaining control over error handling and synchronization. The `allOf().join()` pattern ensures all chunks complete before returning a response.

### Why Batch Duplicate Check?

Instead of checking each deal individually against the database (N queries), we fetch all existing IDs in a single batch query (`SELECT ... WHERE id IN (...)`), then filter in-memory using a `Set`. This reduces database round-trips from O(N) to O(1).

### Why No Transactions Across Chunks?

Since each record failure should be isolated, we don't use `@Transactional` at the chunk level. Each `save()` is auto-committed, and exceptions are caught per-record, ensuring maximum resilience.

## 🐛 Troubleshooting

### Application Won't Start

1. Check if ports 8080 or 5433 are already in use:
   ```bash
   lsof -i :8080
   lsof -i :5433
   ```

2. Check Docker logs:
   ```bash
   make logs
   ```

### Database Connection Issues

1. Verify database is healthy:
   ```bash
   docker-compose ps
   ```

2. Check database logs:
   ```bash
   make db-logs
   ```

3. Test database connection:
   ```bash
   make db-shell
   ```

### Test Failures

If Testcontainers tests fail, ensure Docker is running:
```bash
docker ps
```

## 📈 Performance Tips

1. **Increase Thread Pool Size:** Edit `DealService` constructor to increase threads (default: 10)
2. **Adjust Batch Size:** Modify `BATCH_SIZE` constant (default: 1000)
3. **Tune Database Pool:** Adjust `maximum-pool-size` in `application.yaml` (default: 15)
4. **Enable Hibernate Statistics:** Set `spring.jpa.properties.hibernate.generate_statistics=true`

## 🔒 Database Schema

The application auto-creates the `fx_deals` table using Hibernate DDL:

```sql
CREATE TABLE fx_deals (
    id                BIGSERIAL PRIMARY KEY,
    deal_unique_id    VARCHAR(255) NOT NULL UNIQUE,
    from_currency     VARCHAR(3) NOT NULL,
    to_currency       VARCHAR(3) NOT NULL,
    deal_timestamp    TIMESTAMP NOT NULL,
    deal_amount       NUMERIC(19,2) NOT NULL
);

CREATE UNIQUE INDEX idx_deal_unique_id ON fx_deals(deal_unique_id);
```

## 📝 Environment Variables

| Variable       | Default Value                               | Description                 |
|----------------|---------------------------------------------|-----------------------------|
| `DB_URL`       | `jdbc:postgresql://db:5432/bloomberg_fx`    | PostgreSQL JDBC URL         |
| `DB_USERNAME`  | `postgres`                                  | Database username           |
| `DB_PASSWORD`  | `postgres`                                  | Database password           |

Override these in `docker-compose.yml` or via shell export.

## 🎯 Makefile Commands Reference

| Command           | Description                                      |
|-------------------|--------------------------------------------------|
| `make up`         | Start database & application (detached mode)     |
| `make down`       | Stop and remove containers                       |
| `make logs`       | Follow application logs                          |
| `make test`       | Run all tests (unit + integration)               |
| `make coverage`   | Generate JaCoCo coverage report                  |
| `make clean`      | Clean build artifacts & remove volumes           |
| `make build`      | Build JAR without running tests                  |
| `make restart`    | Restart the application container                |
| `make db-logs`    | View database logs                               |
| `make db-shell`   | Access PostgreSQL CLI                            |
| `make status`     | Check container status                           |

## 🧪 Testing Strategy

### Unit Tests
- **DealServiceTest:** Tests service logic with mocked repository
- Validates batch processing, duplicate handling, and error scenarios

### Integration Tests
- **DealIntegrationTest:** End-to-end tests using Testcontainers
- Spins up a real PostgreSQL container for realistic testing
- Tests full HTTP request → database persistence flow

### Coverage
- JaCoCo generates code coverage metrics
- Report includes line, branch, and method coverage
- View at: `target/site/jacoco/index.html`


## 📄 License

This project is Assignment from ProgressSoft.

## 👥 Authors

Younes Bousfiha

---

**Built with ❤️ using Spring Boot 4 & Java 21**

