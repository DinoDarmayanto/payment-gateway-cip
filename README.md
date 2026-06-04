# payment-gateway-cip

**Version:** `1.0.0`  
**Project Type:** Java Backend Developer Coding Test  
**Java:** `OpenJDK 17.0.18`  
**Build Tool:** `Apache Maven 3.8.7`  
**Database:** `PostgreSQL`  

## Tech Stack

- Java 17
- Spring Boot 3.5.x
- Spring Data JPA / Hibernate
- Flyway
- Spring Security 6
- OAuth2 Resource Server (JWT)
- OpenFeign
- Swagger / OpenAPI
- Lombok
- Kafka
- JUnit 5
- Mockito
- Docker


## Overview

`payment-gateway-cip` adalah service Payment Gateway berbasis Spring Boot yang dibuat untuk coding test Java Backend Developer. Aplikasi ini menerima request pembayaran dari channel seperti Mobile Banking, Internet Banking, atau ATM, lalu memproses transaksi melalui simulasi Core Banking dan Biller Aggregator.

Secara singkat, flow-nya seperti ini:

1. Client mengirim request payment ke Payment Gateway.
2. Payment Gateway melakukan validation dan duplicate order check.
3. Transaksi disimpan dengan status `PENDING`.
4. Service memanggil Core Banking untuk debit account.
5. Jika debit sukses, service melanjutkan ke Biller Aggregator.
6. Hasil transaksi disimpan ke PostgreSQL.
7. Jika transaksi `SUCCESS`, event juga bisa dipublish ke Kafka topic `transaction.success`.

Project ini dibuat untuk menunjukkan implementasi layered architecture, JPA persistence, external service integration with Feign, JWT security, API documentation, error handling, testing, dan deployment readiness.

---

## Architecture

```
      ┌─────────────────────┐
      │ Client / Channel    │
      │ - Mobile Banking    │
      │ - Internet Banking  │
      │ - ATM               │
      └────────┬────────────┘
               │
               ▼
    ┌──────────────────────────────┐
    │ Payment Gateway CIP          │
    │ - REST API                   │
    │ - Validation                 │
    │ - Business Orchestration     │
    │ - JWT Security               │
    │ - Swagger / OpenAPI          │
    └─────────────┬────────────────┘
                  │
        ┌─────────┴────────┐
        ▼                  ▼
┌──────────────────┐  ┌───────────────────┐
│ CoreBankClient   │  │ BillerClient      │
│ - OpenFeign      │  │ - OpenFeign       │
│ - Debit Request  │  │ - Pay Request     │
└────────┬─────────┘  └──────────┬────────┘
         │                       │
         ▼                       ▼
┌──────────────────┐  ┌────────────────────┐
│ Mock Core Bank   │  │ Mock Biller        │
│ - /api/corebank  │  │ - /api/biller/pay  │
│ - SUCCESS/FAILED │  │ - SUCCESS/FAILED   │
└────────┬─────────┘  └──────────┬─────────┘
         └────────────┬──────────┘
                      ▼
  ┌──────────────────────────────────────┐
  │ Application Service Layer            │
  │ - PaymentServiceImpl                 │
  │ - Save PENDING / SUCCESS / FAILED    │
  │ - Publish Kafka event on SUCCESS     │
  │ - Handle fallback and exception flow │
  └──────────────┬───────────────────────┘
                 ▼
     ┌──────────────────────────────┐
     │ PostgreSQL + JPA + Flyway    │
     │ - transactions table         │
     │- transaction history         │
     │ - status tracking            │
     └──────────────┬───────────────┘
                    ▼
  ┌────────────────────────────────────┐
  │ Optional Event Layer               │
  │ - Kafka topic: transaction.success │
  │ - JSON payload                     │
  │ - orderId as message key           │
  └────────────────────────────────────┘
```

### Architecture Summary

| Step | Component | Description |
| ---- | --------- | ----------- |
| 1 | Client / Channel | Mengirim request payment ke endpoint Payment Gateway |
| 2 | Payment Controller | Menerima request API dan meneruskan ke service layer |
| 3 | Payment Service | Menjalankan business flow utama payment transaction |
| 4 | Core Banking | Simulasi debit account customer |
| 5 | Biller Aggregator | Simulasi proses pembayaran ke merchant / biller |
| 6 | PostgreSQL | Menyimpan status transaksi dan reference number |
| 7 | Kafka | Publish event `transaction.success` untuk downstream system |


## Folder Structure

```text
payment-gateway-cip
├── src
│   ├── main
│   │   ├── java/com/cip/api/payment_gateway
│   │   │   ├── Client
│   │   │   │   ├── BillerClient.java
│   │   │   │   └── CoreBankClient.java
│   │   │   ├── Config
│   │   │   │   ├── OpenApiSecurityConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── SwaggerConfig.java
│   │   │   ├── Controller
│   │   │   │   ├── Client
│   │   │   │   │   ├── MockBillerController.java
│   │   │   │   │   └── MockCoreBankController.java
│   │   │   │   └── PaymentController.java
│   │   │   ├── Exception
│   │   │   │   ├── DuplicateOrderException.java
│   │   │   │   ├── ExternalServiceException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── TransactionNotFoundException.java
│   │   │   ├── Model
│   │   │   │   ├── Entity
│   │   │   │   │   └── Transaction.java
│   │   │   │   ├── Enum
│   │   │   │   │   ├── Channel.java
│   │   │   │   │   └── TransactionStatus.java
│   │   │   │   ├── Request
│   │   │   │   │   ├── Client
│   │   │   │   │   │   ├── BillerRequest.java
│   │   │   │   │   │   └── CoreBankRequest.java
│   │   │   │   │   └── PaymentRequest.java
│   │   │   │   └── Response
│   │   │   │       ├── BillerResponse.java
│   │   │   │       ├── CoreBankResponse.java
│   │   │   │       ├── ErrorResponse.java
│   │   │   │       └── PaymentResponse.java
│   │   │   ├── Repository
│   │   │   │   └── TransactionRepository.java
│   │   │   ├── Service
│   │   │   │   ├── impl
│   │   │   │   │   └── PaymentServiceImpl.java
│   │   │   │   └── PaymentService.java
│   │   │   └── PaymentGatewayApplication.java
│   │   └── resources
│   │       ├── application.yaml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── banner.txt
│   │       ├── logback.xml
│   │       └── db/migration/V1__create_transactions_table.sql
│   └── test
│       └── java/com/cip/api/payment_gateway
│           ├── PaymentGatewayApplicationTests.java
│           └── Service/impl/PaymentServiceImplTest.java
├── docs
│   ├── architecture-diagram.md
│   ├── compliance-report.md
│   └── sequence-diagram.md
├── .env.example
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

## Architecture Diagram

See:

- [docs/architecture-diagram.md](docs/architecture-diagram.md)
- [docs/sequence-diagram.md](docs/sequence-diagram.md)


## Prerequisites

- Java 17
- Maven 3.8+
- PostgreSQL 14+ or compatible
- Optional: Keycloak or another JWT issuer for strict security mode
-  Docker and Docker Compose

## Database Setup

Create a PostgreSQL database and user, for example:

```sql
CREATE DATABASE payment_gateway;
CREATE USER payment_user WITH PASSWORD 'payment123';
GRANT ALL PRIVILEGES ON DATABASE payment_gateway TO payment_user;
```

The application uses Flyway migration:

- `src/main/resources/db/migration/V1__create_transactions_table.sql`

## Environment Configuration

You can copy values from `.env.example` and export them before running the application.

Main variables:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `COREBANK_URL`
- `BILLER_URL`
- `JWT_ISSUER_URI`
- `JWT_JWK_SET_URI`
- `APP_SECURITY_PERMIT_ALL`

## Application Startup

### Local development mode without JWT

This mode is useful when Keycloak or another identity provider is not available locally.

```bash
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

Or:

```bash
APP_SECURITY_PERMIT_ALL=true mvn spring-boot:run
```

### Production-like mode with JWT

```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

Or:

```bash
APP_SECURITY_PERMIT_ALL=false mvn spring-boot:run
```

### Build executable jar

```bash
mvn clean package
```

### Run jar

```bash
java -jar target/payment-gateway-cip-0.0.1-SNAPSHOT.jar
```

## Swagger Access

Swagger UI is available at:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/swagger-ui/index.html`

OpenAPI JSON is available at:

- `http://localhost:8080/v3/api-docs`

Swagger remains public in both `dev` and `prod` profiles.

## JWT Authentication

Security rules:

- Public:
  - `/swagger-ui/**`
  - `/swagger-ui.html`
  - `/v3/api-docs/**`
  - `POST /api/corebank/**`
  - `POST /api/biller/**`
- Protected:
  - `POST /api/payments`
  - `GET /api/payments/{id}`

JWT configuration is sourced from:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:...}
          jwk-set-uri: ${JWT_JWK_SET_URI:...}
```

In Swagger, click `Authorize` and enter a bearer token in strict mode.

## API Usage

### POST /api/payments

Create a new payment transaction.

Request:

```json
{
  "orderId": "INV-12345",
  "channel": "MOBILE_BANKING",
  "amount": 250000,
  "account": "1234567890",
  "currency": "IDR",
  "paymentMethod": "VIRTUAL_ACCOUNT"
}
```

Success response:

```json
{
  "transactionId": "1fd5c93a-5243-47b6-a1a1-9884f6ecf0cb",
  "orderId": "INV-12345",
  "status": "SUCCESS",
  "corebankReference": "CB123456789",
  "billerReference": "BILLER987654321",
  "message": null
}
```

Failed response example:

```json
{
  "transactionId": "1fd5c93a-5243-47b6-a1a1-9884f6ecf0cb",
  "orderId": "INV-12345",
  "status": "FAILED",
  "corebankReference": null,
  "billerReference": null,
  "message": "Insufficient balance"
}
```

Duplicate order example:

```json
{
  "timestamp": "2026-06-04T07:30:00",
  "status": 409,
  "message": "Order ID already exists",
  "path": "/api/payments"
}
```

`curl` example in JWT mode:

```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "INV-12345",
    "channel": "MOBILE_BANKING",
    "amount": 250000,
    "account": "1234567890",
    "currency": "IDR",
    "paymentMethod": "VIRTUAL_ACCOUNT"
  }'
```

### GET /api/payments/{id}

Get transaction status by UUID.

Success response:

```json
{
  "transactionId": "1fd5c93a-5243-47b6-a1a1-9884f6ecf0cb",
  "orderId": "INV-12345",
  "status": "SUCCESS",
  "corebankReference": "CB123456789",
  "billerReference": "BILLER987654321",
  "message": null
}
```

Not found example:

```json
{
  "timestamp": "2026-06-04T07:30:00",
  "status": 404,
  "message": "Transaction not found",
  "path": "/api/payments/1fd5c93a-5243-47b6-a1a1-9884f6ecf0cb"
}
```

`curl` example in JWT mode:

```bash
curl -i http://localhost:8080/api/payments/1fd5c93a-5243-47b6-a1a1-9884f6ecf0cb \
  -H "Authorization: Bearer <token>"
```

## Testing Instructions

Run all tests:

```bash
mvn test
```

Compile tests only:

```bash
mvn -DskipTests test-compile
```

Run full build:

```bash
mvn clean package
```

Current automated tests include:

- application context smoke test
- unit test for `PaymentServiceImpl`
- integration test for payment flow using `MockMvc`
- integration test for security access rules
- fallback test for `BillerGatewayServiceImpl`
- JaCoCo coverage reporting

## Changelog
### v1.0.0

- Initial release of `payment-gateway-cip`
- Add layered architecture: Controller, Service, Repository, DTO, Entity
- Add PostgreSQL persistence with Spring Data JPA / Hibernate
- Add Flyway migration for `transactions` table
- Add payment orchestration flow: Core Banking -> Biller -> Save DB
- Add OpenFeign integration for CoreBank and Biller clients
- Add structured exception handling with global error response
- Add JWT security with OAuth2 Resource Server
- Add Swagger / OpenAPI with Bearer Auth support
- Add Dockerfile, `docker-compose.yml`, and `.env.example`
- Add Kafka `transaction.success` event publishing
- Add Resilience4j retry + circuit breaker for Biller integration
- Add unit and integration tests with JaCoCo reporting


## License

This project was created for internal coding test and evaluation purposes.

License / ownership: `CIP`
