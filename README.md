# RecoverAI

RecoverAI is an AI-powered revenue recovery system for failed payment transactions built for the Razorpay Buildathon (Track 3). It autonomously detects at-risk revenue, diagnoses payment failures with Google Gemini (gemini-3.7-flash), applies deterministic safety and compliance policies, executes bounded recovery actions (such as smart retries and personalized payment link generation), measures recovered revenue, and maintains an immutable audit trail.

---

## Tech Stack

### Backend
- **Language & Framework**: Java 21, Spring Boot 3.4.x
- **Build Tool**: Maven
- **Persistence & Migration**: Spring Data JPA, PostgreSQL 16, Flyway
- **Observability & Diagnostics**: Spring Boot Actuator
- **Validation**: Hibernate Validator (Spring Boot Starter Validation)

### Frontend
- **Framework & Tooling**: React 19, Vite, TypeScript
- **Styling & UI**: Tailwind CSS v4, Lucide Icons, Recharts (planned)

### AI & Integrations
- **AI Model**: Google Gemini API (`gemini-3.7-flash`)
- **Payment Gateway**: Razorpay API & Webhooks

### Infrastructure & DevOps
- **Containerization**: Docker & Docker Compose
- **Version Control**: Git

---

## Local Setup Prerequisites

- **Java JDK**: Version 21 (JDK 21 LTS required)
- **Node.js**: Version 20+ (with npm 10+)
- **Apache Maven**: Version 3.9+ (or use included `mvnw`)
- **Docker & Docker Compose**: For local PostgreSQL database (optional if running external PostgreSQL)

---

## Environment Variables

Copy `.env.example` to `.env` in the root directory (or configure system environment variables):

```bash
cp .env.example .env
```

### Backend Required Variables
| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `SERVER_PORT` | HTTP server port | `8080` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated CORS origins | `http://localhost:5173,http://localhost:3000` |
| `DATABASE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/recoverai` |
| `DATABASE_USERNAME` | PostgreSQL database user | `postgres` |
| `DATABASE_PASSWORD` | PostgreSQL database password | `postgres` |
| `GEMINI_API_KEY` | Google Gemini API key | `<your_gemini_api_key>` |
| `GEMINI_MODEL` | Gemini model identifier | `gemini-3.7-flash` |
| `RAZORPAY_KEY_ID` | Razorpay Key ID | `<your_razorpay_key_id>` |
| `RAZORPAY_KEY_SECRET` | Razorpay Key Secret | `<your_razorpay_key_secret>` |

### Frontend Required Variables
In `frontend/.env` (or `frontend/.env.example`):
| Variable | Description | Default |
| :--- | :--- | :--- |
| `VITE_API_BASE_URL` | Base URL of the backend API | `http://localhost:8080` |

---

## Getting Started

### 1. Start Local Database (PostgreSQL)

If using Docker Compose:
```bash
docker compose up -d
```
This starts PostgreSQL 16 on port `5432` with database `recoverai`.

### 2. Run Backend (Spring Boot)

Navigate to the `backend` directory and run:
```bash
cd backend
mvn spring-boot:run
```

The backend server will start on port `8080`.
- Health Check: `GET http://localhost:8080/api/v1/health`
- Actuator Health: `GET http://localhost:8080/actuator/health`

### 3. Run Frontend (React + Vite)

In a separate terminal, navigate to the `frontend` directory:
```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) in your browser.

---

## Running Tests

### Backend Tests
Execute unit and integration tests (including Flyway migration verification and health endpoint tests):
```bash
cd backend
mvn clean test
```

### Frontend Build & Lint
Compile TypeScript and bundle frontend assets:
```bash
cd frontend
npm run build
```

---

---

## Razorpay Webhook Ingestion

RecoverAI includes a multi-tenant webhook ingestion pipeline for Razorpay payment events.

### Endpoint Specification
- **URL**: `POST /api/v1/webhooks/razorpay`
- **Content-Type**: `application/json`
- **Header**: `X-Razorpay-Signature: <hmac_sha256_hex>`

### Supported Events
- `payment.failed`: Persists/updates customer and payment record, deterministically classifies failure category, determines priority based on amount tiers, and creates an `OPEN` `RecoveryCase`.
- `payment.captured`: Updates payment status to `CAPTURED`, records audit event, does not create a recovery case.
- `payment.authorized`: Updates payment status to `AUTHORIZED`.
- `payment.refunded`: Updates payment status to `REFUNDED`.
- *Other Razorpay events*: Safely recorded in `webhook_events` as `IGNORED` and accepted with HTTP 200 without crashing or corrupting state.

### Architecture & Security Guarantees
1. **Constant-Time Signature Verification**: HMAC-SHA256 signature is computed against the raw request body with the merchant's configured webhook secret and compared using `MessageDigest.isEqual` to prevent timing attacks.
2. **Zero Secret Leakage**: Webhook secrets and API keys are never logged, returned in responses, or persisted in audit details.
3. **Multi-Tenant Isolation**: The webhook resolves the merchant via `payload.account_id` matching `merchants.razorpay_account_id`. Signature verification is evaluated exclusively against the resolved merchant's secret.
4. **Idempotency & Duplicate Protection**: Every webhook receipt is hashed (`SHA-256`) and recorded in the `webhook_events` table (Flyway V3 migration). Retried or duplicated events are acknowledged with HTTP 200 `{ "status": "accepted" }` without re-mutating payment or case state.
5. **Deterministic Failure Categorization**:
   - `insufficient_funds`: Balance or limit-related errors
   - `authentication_failure`: 3D Secure, OTP, PIN, 2FA failures
   - `network_error`: Gateway, timeout, server errors
   - `bank_declined`: Issuer bank declines, do not honor
   - `invalid_request`: Bad request or invalid card data
   - `unknown`: Unclassified fallback

---

## Local Webhook Testing Guide

To test the webhook locally:

### 1. Compute HMAC-SHA256 Signature (PowerShell Example)
```powershell
$secret = "your_merchant_webhook_secret"
$body = '{"entity":"event","account_id":"acc_test123","event_id":"evt_001","event":"payment.failed","contains":["payment"],"payload":{"payment":{"entity":{"id":"pay_test_001","entity":"payment","amount":500000,"currency":"INR","status":"failed","order_id":"order_001","method":"card","email":"customer@example.com","contact":"+919876543210","customer_id":"cust_001","error_code":"BAD_REQUEST_ERROR","error_description":"Insufficient funds in account","error_source":"bank","error_reason":"insufficient_funds","created_at":1600000000}}}}'

$hmac = New-Object System.Security.Cryptography.HMACSHA256
$hmac.Key = [System.Text.Encoding]::UTF8.GetBytes($secret)
$hash = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($body))
$signature = -join ($hash | ForEach-Object { "{0:x2}" -f $_ })
```

### 2. Send Webhook Request via Curl / Invoke-RestMethod
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/webhooks/razorpay" `
  -Method Post `
  -Headers @{ "X-Razorpay-Signature" = $signature; "Content-Type" = "application/json" } `
  -Body $body
```

---

## AI Failure Diagnosis Engine

RecoverAI incorporates an AI-driven failure diagnosis engine powered by Google Gemini (`gemini-3.7-flash`). When a payment failure creates a `RecoveryCase`, the diagnosis engine analyzes the error codes, payment method, risk tier, amount, and failure category to produce a structured, validated recovery recommendation persisted in `AgentDecision`.

### Diagnostic Flow
```
Failed Payment ──► RecoveryCase ──► AIDiagnosisService ──► GeminiClient ──► Structured Output ──► AgentDecision & AuditEvent
```

### Endpoint Specification
- **URL**: `POST /api/v1/recovery-cases/{recoveryCaseId}/diagnose`
- **Header**: `X-Merchant-Id: <uuid>`
- **Alternative URL**: `POST /api/v1/merchants/{merchantId}/recovery-cases/{recoveryCaseId}/diagnose`

### Structured AI Output Schema
```json
{
  "id": "uuid",
  "recoveryCaseId": "uuid",
  "merchantId": "uuid",
  "recommendedAction": "SEND_PAYMENT_LINK | RETRY_CHARGE | SWITCH_PAYMENT_METHOD | MANUAL_INTERVENTION",
  "channel": "WHATSAPP | EMAIL | SMS | RETRY_CHARGE | SMART_LINK | MANUAL",
  "confidenceScore": 0.8850,
  "reasoning": "Detailed diagnostic reasoning",
  "decisionFactors": "{\"primaryReason\":\"insufficient_funds\",\"retryViability\":\"HIGH\"}",
  "modelName": "gemini-3.7-flash",
  "modelVersion": "gemini-3.7-flash-001",
  "promptTokens": 240,
  "completionTokens": 65,
  "createdAt": "2026-08-27T22:45:00Z"
}
```

### Key Guarantees
1. **Multi-Tenant Scoping**: Recovery cases are strictly loaded using merchant-scoped queries (`findByIdAndMerchantId`). Cross-tenant access is rejected with 404/400.
2. **Confidence Score Boundary Validation**: Scores are strictly validated in range `0.0 <= score <= 1.0` before saving.
3. **Structured JSON Mode**: Gemini's `application/json` mode ensures strictly valid JSON structure.
4. **PII Masking**: Customer emails and sensitive identifiers are masked before dispatching context to the LLM.
5. **Zero Credential Exposure**: `GEMINI_API_KEY` is loaded from environment variables and never logged or exposed in exceptions.
6. **Immutable Audit Trail**: Every AI decision is recorded in `audit_events` under `ActorType.AGENT`.

---

## Recovery Orchestration Layer

RecoverAI provides an automated and bounded Recovery Orchestration layer (`RecoveryOrchestratorService`) that consumes AI `AgentDecision` records, evaluates domain lifecycle constraints, safely sequences sequential attempts, and dispatches recovery actions through a clean channel executor abstraction.

### Orchestration Flow
```
RecoveryCase (OPEN) ──► AgentDecision ──► RecoveryOrchestratorService ──► RecoveryActionExecutor ──► RecoveryAttempt & AuditEvent
                                                     │
                                                     ├── Sequences attempt_number (DB-backed)
                                                     ├── Case state: OPEN ➔ IN_PROGRESS
                                                     ├── Attempt state: SCHEDULED ➔ IN_FLIGHT ➔ SENT / SUCCESS / FAILED
                                                     └── Multi-tenant isolation & idempotency guard
```

### Endpoint Specification
- **URL**: `POST /api/v1/recovery-cases/{recoveryCaseId}/orchestrate`
- **Header**: `X-Merchant-Id: <uuid>`
- **Alternative URL**: `POST /api/v1/merchants/{merchantId}/recovery-cases/{recoveryCaseId}/orchestrate`

### Orchestration Response Schema
```json
{
  "id": "uuid",
  "recoveryCaseId": "uuid",
  "merchantId": "uuid",
  "attemptNumber": 1,
  "channel": "WHATSAPP",
  "status": "SENT",
  "scheduledAt": "2026-08-28T00:00:00Z",
  "executedAt": "2026-08-28T00:00:01Z",
  "completedAt": "2026-08-28T00:00:01Z",
  "resultCode": "WHATSAPP_DISPATCHED",
  "resultMessage": "Simulated WhatsApp message dispatched to customer",
  "recoveryLink": "https://pay.recoverai.io/r/{caseId}",
  "createdAt": "2026-08-28T00:00:00Z",
  "updatedAt": "2026-08-28T00:00:01Z"
}
```

### Key Guarantees
1. **DB-Backed Safe Sequencing**: Attempt numbers are sequentially computed from persistent database records (`findTopByRecoveryCaseIdOrderByAttemptNumberDesc`) backed by the database uniqueness constraint `(recovery_case_id, attempt_number)`.
2. **Idempotency & Concurrent Conflict Protection**: Active attempts in `SCHEDULED` or `IN_FLIGHT` state block duplicate orchestration calls (HTTP 409 Conflict).
3. **Lifecycle Management**:
   - `RecoveryCase` transitions from `OPEN` to `IN_PROGRESS` upon initiation.
   - `RecoveryAttempt` transitions from `SCHEDULED` ➔ `IN_FLIGHT` ➔ `SENT` / `SUCCESS` / `FAILED` / `SKIPPED`.
   - On immediate success (e.g. `RETRY_CHARGE`), the case status advances to `RECOVERED`.
4. **Terminal Case Guard**: Cases already in `RECOVERED`, `CANCELLED`, or `EXPIRED` status reject further recovery attempts (HTTP 400 Bad Request).
5. **Channel Executor Abstraction**: `RecoveryActionExecutor` decouples channel dispatch mechanics (`WHATSAPP`, `EMAIL`, `SMS`, `RETRY_CHARGE`, `SMART_LINK`, `MANUAL`) with zero provider-specific API coupling and simulated mock dispatching.
6. **Multi-Tenant Security**: Strict merchant verification on every query; AI raw responses and merchant credentials are never returned in public DTOs.
7. **Audit Trail**: Records structured events (`RECOVERY_ATTEMPT_CREATED`, `RECOVERY_ATTEMPT_STARTED`, `RECOVERY_ATTEMPT_SENT`, `RECOVERY_ATTEMPT_SUCCEEDED`, `RECOVERY_ATTEMPT_FAILED`) under `ActorType.SYSTEM`.

---

## Recovery Communication & Execution Layer

RecoverAI features a modular, extensible communication and recovery execution layer that decouples domain orchestration from external messaging networks (WhatsApp, Email, SMS) and payment gateways (Razorpay).

### Architecture & Provider Abstraction
```
RecoveryOrchestratorService
           │
           ├── WhatsAppRecoveryExecutor  ──► WhatsAppProvider (MockWhatsAppProvider / Twilio / Meta Cloud API)
           ├── EmailRecoveryExecutor     ──► EmailProvider (MockEmailProvider / SendGrid / Postmark)
           ├── SmsRecoveryExecutor       ──► SmsProvider (MockSmsProvider / Twilio / Kaleyra)
           ├── SmartLinkRecoveryExecutor ──► RecoveryLinkService (Configurable Safe URL Generator)
           ├── RetryChargeRecoveryExecutor ─► PaymentRetryProvider (MockPaymentRetryProvider / Razorpay API)
           ├── ManualRecoveryExecutor    ──► RecoveryLinkService (Queued Manual Review)
           └── DefaultRecoveryActionExecutor (Fallback)
```

### Supported Channels & Behavior
| Channel | Executor Class | Underlying Provider | Case Status Transition | Attempt Status |
| :--- | :--- | :--- | :--- | :--- |
| `WHATSAPP` | `WhatsAppRecoveryExecutor` | `WhatsAppProvider` | `OPEN` ➔ `IN_PROGRESS` | `SENT` |
| `EMAIL` | `EmailRecoveryExecutor` | `EmailProvider` | `OPEN` ➔ `IN_PROGRESS` | `SENT` |
| `SMS` | `SmsRecoveryExecutor` | `SmsProvider` | `OPEN` ➔ `IN_PROGRESS` | `SENT` |
| `SMART_LINK` | `SmartLinkRecoveryExecutor` | `RecoveryLinkService` | `OPEN` ➔ `IN_PROGRESS` | `SENT` |
| `RETRY_CHARGE` | `RetryChargeRecoveryExecutor` | `PaymentRetryProvider` | `OPEN` ➔ `RECOVERED` (on success) | `SUCCESS` / `FAILED` |
| `MANUAL` | `ManualRecoveryExecutor` | `RecoveryLinkService` | `OPEN` ➔ `IN_PROGRESS` | `SENT` |

### Local & Test Providers
All communication providers default to safe, deterministic local mock implementations (`MockWhatsAppProvider`, `MockEmailProvider`, `MockSmsProvider`, `MockPaymentRetryProvider`):
- **Zero Real External Calls**: No live messages are dispatched and no live cards are charged during local development or test suite execution.
- **PII-Safe Logging**: Phone numbers and email addresses are masked before writing to server logs (e.g. `+919876****10`, `a***@example.com`).
- **Deterministic Metadata**: Simulated delivery IDs (`mock_wa_...`, `mock_email_...`, `mock_pay_...`) and JSON metadata payloads are returned for full lifecycle verification.

### Safe Recovery Link Generation
The `RecoveryLinkService` generates recovery URLs with configurable base URLs (`recoverai.recovery.base-url`):
- Formats links cleanly as `${baseUrl}${caseId}` without exposing internal secrets, API keys, or raw merchant tokens.
- Supports customizable domain routing in development (`http://localhost:5173/r/`) and production (`https://pay.recoverai.io/r/`).

### Configuration Properties
```yaml
recoverai:
  recovery:
    base-url: ${RECOVERY_BASE_URL:https://pay.recoverai.io/r/}
    whatsapp:
      provider: ${WHATSAPP_PROVIDER:mock}
      sender-number: ${WHATSAPP_SENDER_NUMBER:+14155238886}
      api-key: ${WHATSAPP_API_KEY:}
    email:
      provider: ${EMAIL_PROVIDER:mock}
      from-address: ${EMAIL_FROM_ADDRESS:recover@recoverai.io}
      from-name: ${EMAIL_FROM_NAME:RecoverAI Payment Recovery}
      api-key: ${EMAIL_API_KEY:}
    sms:
      provider: ${SMS_PROVIDER:mock}
      sender-id: ${SMS_SENDER_ID:RECOVER}
      api-key: ${SMS_API_KEY:}
    retry-charge:
      provider: ${RETRY_CHARGE_PROVIDER:mock}
      auto-retry-enabled: ${RETRY_CHARGE_ENABLED:true}
```

---

## Current Project Status

- **Implemented Features**:
  - `feature/project-foundation`: Spring Boot 3.4.x + Java 21 foundation, Actuator health, CORS, Vite + React shell.
  - `feature/database-schema`: Core domain entities (Merchant, Customer, Payment, RecoveryCase, RecoveryAttempt, AgentDecision, AuditEvent) with Flyway V1 and V2 migrations.
  - `feature/razorpay-payment-ingestion`: Robust Razorpay webhook ingestion endpoint (`POST /api/v1/webhooks/razorpay`), HMAC-SHA256 constant-time verification, multi-tenant merchant resolution, customer/payment upserting, Flyway V3 `webhook_events` idempotency tracking, deterministic failure categorization, RecoveryCase generation, and audit logging.
  - `feature/ai-failure-diagnosis-engine`: Google Gemini AI failure diagnosis engine (`AIDiagnosisService`, `GeminiClient`, `AIDiagnosisController`), structured JSON recommendations, confidence score validation, tenant isolation, PII masking, `AgentDecision` persistence.
  - `feature/recovery-orchestration`: Recovery Orchestration layer (`RecoveryOrchestratorService`, `RecoveryActionExecutor`, `DefaultRecoveryActionExecutor`, `RecoveryOrchestrationController`), lifecycle state machine, DB-backed attempt sequencing, duplicate protection, multi-tenant scoping, and audit logging.
  - `feature/recovery-communication`: Recovery Communication & Execution Layer (`WhatsAppRecoveryExecutor`, `EmailRecoveryExecutor`, `SmsRecoveryExecutor`, `SmartLinkRecoveryExecutor`, `RetryChargeRecoveryExecutor`, `ManualRecoveryExecutor`, `DefaultRecoveryLinkService`, safe mock providers, configuration properties, and 122 automated tests passing).



