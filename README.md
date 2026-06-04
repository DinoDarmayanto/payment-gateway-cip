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



## Configuration

Bagian ini menjelaskan konfigurasi utama dan profile-specific configuration yang dipakai oleh `payment-gateway-cip`.

### Main Configuration (`application.yaml`)

File ini adalah base configuration yang dipakai secara default saat aplikasi dijalankan.

```yaml
spring:
  application:
    name: payment-gateway-cip

  banner:
    location: classpath:banner.txt

  datasource:
    url: jdbc:postgresql://localhost:5432/payment_gateway
    username: payment_user
    password: payment123
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false

  flyway:
    enabled: true
    locations: classpath:db/migration

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:http://localhost:8081/realms/payment-gateway}
          jwk-set-uri: ${JWT_JWK_SET_URI:http://localhost:8081/realms/payment-gateway/protocol/openid-connect/certs}

server:
  port: 8080

integration:

  corebank:
    url: ${COREBANK_URL:http://localhost:8080}

  biller:
    url: ${BILLER_URL:http://localhost:8080}

springdoc:
  swagger-ui:
    path: /swagger-ui.html



management:
  endpoints:
    web:
      exposure:
        include: health,info

logging:
  level:
    com.cip.api.payment_gateway: DEBUG

app:
  kafka:
    enabled: ${APP_KAFKA_ENABLED:true}
    topic:
      transaction-success: ${KAFKA_TOPIC_TRANSACTION_SUCCESS:transaction.success}
  security:
    permit-all: ${APP_SECURITY_PERMIT_ALL:false}

resilience4j:
  circuitbreaker:
    instances:
      billerClient:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 3
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 2
  retry:
    instances:
      billerClient:
        max-attempts: 3
        wait-duration: 2s
        retry-exceptions:
          - feign.FeignException

```

### Development Profile (`application-dev.yml`)

Dipakai untuk local development mode, terutama saat Anda belum menjalankan Keycloak atau JWT issuer lain.

```yaml
app:
  security:
    permit-all: true
```

### Production Profile (`application-prod.yml`)

Dipakai untuk mode yang lebih strict dan lebih cocok untuk demo atau submission mode.

```yaml
app:
  security:
    permit-all: false
```

### Environment Variables

Berikut daftar environment variable utama yang dipakai oleh `payment-gateway-cip`. Tabel ini membantu saat menjalankan aplikasi secara local, via Docker, maupun saat demo interview.

| Variable | Default Value | Description |
| --- | --- | --- |
| `DB_HOST` | `localhost` | Host PostgreSQL utama |
| `DB_PORT` | `5432` | Port PostgreSQL |
| `DB_NAME` | `payment_gateway` | Nama database aplikasi |
| `DB_USERNAME` | `payment_user` | Username database |
| `DB_PASSWORD` | `payment123` | Password database |
| `COREBANK_URL` | `http://localhost:8080` | Base URL mock atau downstream Core Banking |
| `BILLER_URL` | `http://localhost:8080` | Base URL mock atau downstream Biller Aggregator |
| `JWT_ISSUER_URI` | `http://localhost:8081/realms/payment-gateway` | Issuer URI untuk validasi Bearer JWT |
| `JWT_JWK_SET_URI` | `http://localhost:8081/realms/payment-gateway/protocol/openid-connect/certs` | JWK Set URI untuk public key JWT |
| `APP_SECURITY_PERMIT_ALL` | `false` | `true` untuk dev mode tanpa auth, `false` untuk protected JWT mode |
| `APP_KAFKA_ENABLED` | `true` | Mengaktifkan publish event Kafka saat transaksi `SUCCESS` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `KAFKA_TOPIC_TRANSACTION_SUCCESS` | `transaction.success` | Nama topic untuk success transaction event |

## Environment Configuration

Contoh isi file `.env` atau environment shell yang bisa dipakai untuk local run maupun container deployment:

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=payment_gateway
DB_USERNAME=payment_user
DB_PASSWORD=payment123

COREBANK_URL=http://localhost:8080
BILLER_URL=http://localhost:8080

JWT_ISSUER_URI=http://localhost:8081/realms/payment-gateway
JWT_JWK_SET_URI=http://localhost:8081/realms/payment-gateway/protocol/openid-connect/certs

APP_SECURITY_PERMIT_ALL=false
```

Catatan:

- Untuk local development tanpa Keycloak, set `APP_SECURITY_PERMIT_ALL=true`.
- Untuk mode yang lebih production-like, gunakan `APP_SECURITY_PERMIT_ALL=false` dan isi `JWT_ISSUER_URI` serta `JWT_JWK_SET_URI` dengan issuer yang valid.
- Referensi variabel ini juga sudah disiapkan di file `.env.example`.

### Main Docker (`Dockerfile`)

`Dockerfile` ini digunakan untuk membangun image aplikasi Spring Boot berbasis Java 17. Konfigurasinya sengaja dibuat sederhana, kecil, dan cocok untuk executable JAR hasil `mvn clean package`.

```dockerfile
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ARG JAR_FILE=target/payment-gateway-cip-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Keterangan singkat:

- Base image menggunakan `eclipse-temurin:17-jre-jammy` agar runtime tetap ringan.
- Artifact yang dijalankan adalah JAR hasil build Maven.
- Container expose port `8080` sesuai port aplikasi.

### Docker Compose App (`docker-compose.yml`)

File ini menyalakan dua service utama: `postgres` dan `payment-gateway-cip`. Cocok untuk menjalankan aplikasi full stack secara local tanpa setup manual database.

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: payment-gateway-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME:-payment_gateway}
      POSTGRES_USER: ${DB_USERNAME:-payment_user}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-payment123}
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - payment_gateway_net

  payment-gateway-cip:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: payment-gateway-cip
    restart: unless-stopped
    depends_on:
      - postgres
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
      DB_HOST: ${DB_HOST:-postgres}
      DB_PORT: ${DB_PORT:-5432}
      DB_NAME: ${DB_NAME:-payment_gateway}
      DB_USERNAME: ${DB_USERNAME:-payment_user}
      DB_PASSWORD: ${DB_PASSWORD:-payment123}
      COREBANK_URL: ${COREBANK_URL:-http://payment-gateway-cip:8080}
      BILLER_URL: ${BILLER_URL:-http://payment-gateway-cip:8080}
      JWT_ISSUER_URI: ${JWT_ISSUER_URI:-http://host.docker.internal:8081/realms/payment-gateway}
      JWT_JWK_SET_URI: ${JWT_JWK_SET_URI:-http://host.docker.internal:8081/realms/payment-gateway/protocol/openid-connect/certs}
      APP_SECURITY_PERMIT_ALL: ${APP_SECURITY_PERMIT_ALL:-false}
      SERVER_PORT: 8080
      SPRING_DATASOURCE_URL: jdbc:postgresql://${DB_HOST:-postgres}:${DB_PORT:-5432}/${DB_NAME:-payment_gateway}
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME:-payment_user}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-payment123}
    ports:
      - "8080:8080"
    networks:
      - payment_gateway_net

volumes:
  postgres_data:

networks:
  payment_gateway_net:
    driver: bridge

```

Keterangan singkat:

- `postgres` menggunakan volume persisten `postgres_data` agar data tidak hilang saat container restart.
- `payment-gateway-cip` membaca konfigurasi dari environment variable sehingga mudah dipindah dari local ke container.
- `SPRING_PROFILES_ACTIVE` default ke `prod`, tetapi bisa diubah sesuai kebutuhan demo.
- `SPRING_DATASOURCE_URL`, username, dan password sudah di-override agar aplikasi langsung mengarah ke service PostgreSQL dalam network Docker.

Cara menjalankan:

```bash
docker compose up --build
```

### Docker Compose Kafka (`docker-compose.kafka.yml`)

File tambahan ini dipakai khusus untuk bonus feature Kafka. Isinya menyalakan broker Kafka berbasis KRaft dan `kafka-ui` untuk memverifikasi event `transaction.success`.

```yaml
services:
  kafka:
    image: bitnami/kafka:3.9
    container_name: payment-gateway-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_CFG_NODE_ID: 1
      KAFKA_CFG_PROCESS_ROLES: broker,controller
      KAFKA_CFG_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CFG_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_CFG_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_CFG_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_CFG_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_KRAFT_CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
      ALLOW_PLAINTEXT_LISTENER: "yes"

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: payment-gateway-kafka-ui
    depends_on:
      - kafka
    ports:
      - "8081:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092

```

Keterangan singkat:

- Kafka broker diekspos ke `localhost:9092`.
- Kafka UI dapat diakses di `http://localhost:8081`.
- Topic akan otomatis dibuat jika belum ada, sehingga cocok untuk local verification.

Cara menjalankan:

```bash
docker compose -f docker-compose.kafka.yml up -d
```


## Database Setup

Bagian ini menjelaskan setup PostgreSQL yang dibutuhkan agar `payment-gateway-cip` bisa berjalan dengan baik di local environment maupun container environment.

Masuk ke PostgreSQL terlebih dahulu:

```bash
psql -U postgres
```

Setelah masuk ke console PostgreSQL, jalankan perintah berikut untuk membuat database dan user aplikasi:

```sql
CREATE DATABASE payment_gateway;
CREATE USER payment_user WITH PASSWORD 'payment123';
GRANT ALL PRIVILEGES ON DATABASE payment_gateway TO payment_user;
```

Keterangan:

- `payment_gateway` adalah database utama untuk menyimpan data transaksi payment gateway.
- `payment_user` adalah database user yang dipakai oleh aplikasi Spring Boot.
- Nilai ini sebaiknya konsisten dengan konfigurasi di `application.yaml`, `.env`, dan `docker-compose.yml`.

Project ini menggunakan Flyway migration, sehingga tabel tidak perlu dibuat manual satu per satu. Saat aplikasi startup, Flyway akan mengeksekusi script migration secara otomatis.

File migration utama berada di:

- `src/main/resources/db/migration/V1__create_transactions_table.sql`

DDL utama yang digunakan pada migration tersebut adalah:

```sql
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL UNIQUE,
    channel VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    account VARCHAR(50) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'IDR',
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    corebank_reference VARCHAR(100),
    biller_reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Penjelasan struktur tabel:

- `id` digunakan sebagai primary key berbasis UUID.
- `order_id` wajib unik untuk mencegah duplicate order.
- `channel`, `amount`, `account`, `currency`, dan `payment_method` menyimpan data request dari channel/client.
- `status` menyimpan state transaksi seperti `PENDING`, `FAILED`, atau `SUCCESS`.
- `corebank_reference` dan `biller_reference` menyimpan reference number dari downstream service.
- `created_at` dan `updated_at` digunakan untuk audit trail dan tracking perubahan data.

Secara singkat, database flow-nya adalah:

1. Buat database dan user PostgreSQL.
2. Jalankan aplikasi Spring Boot.
3. Flyway mengeksekusi migration otomatis.
4. Tabel `transactions` siap digunakan oleh JPA/Hibernate.

## Prerequisites

- Java 17
- Maven 3.8+
- PostgreSQL 14+ atau versi compatible
- Optional: Keycloak atau JWT issuer lain untuk strict JWT validation mode
- Optional: Kafka jika ingin menguji event publishing dan Kafka UI
- Docker dan Docker Compose untuk containerized setup

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


## API Documentation

Dokumentasi API tersedia melalui Swagger UI setelah aplikasi berhasil dijalankan. Section ini membantu reviewer atau interviewer untuk melihat daftar endpoint, schema request/response, dan mencoba request langsung dari browser.

Swagger UI:

- Local URL: `http://localhost:8080/swagger-ui.html`
- Alternative URL: `http://localhost:8080/swagger-ui/index.html`

Melalui Swagger UI, Anda bisa:

- melihat seluruh endpoint yang tersedia
- memeriksa request body dan response schema
- menguji API secara langsung
- mengisi Bearer token melalui tombol `Authorize` saat menjalankan mode JWT

OpenAPI JSON:

- `http://localhost:8080/v3/api-docs`

Catatan:

- Swagger tetap dapat diakses tanpa login pada profile `dev` maupun `prod`.
- Endpoint payment tetap mengikuti rule security yang aktif, sehingga pada mode strict Anda tetap perlu Bearer token untuk mengakses endpoint protected.

## API Endpoints

Berikut endpoint utama yang tersedia pada project `payment-gateway-cip`:

| Endpoint | Method | Description |
| --- | --- | --- |
| `/api/payments` | `POST` | Membuat transaksi pembayaran baru dan menjalankan flow validasi, debit, biller, lalu simpan hasil transaksi |
| `/api/payments/{id}` | `GET` | Mengambil detail transaksi berdasarkan `transactionId` |
| `/api/corebank/debit` | `POST` | Mock endpoint untuk simulasi debit ke Core Banking |
| `/api/biller/pay` | `POST` | Mock endpoint untuk simulasi pembayaran ke Biller Aggregator |

Keterangan security:

- `/api/payments` dan `/api/payments/{id}` adalah endpoint utama yang diproteksi oleh JWT saat mode security strict aktif.
- `/api/corebank/debit` dan `/api/biller/pay` dibiarkan public untuk kebutuhan simulasi downstream service.

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
