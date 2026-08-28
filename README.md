# RecoverAI

RecoverAI is an AI-powered revenue recovery system for failed payment transactions built for the Razorpay Buildathon (Track 3). It autonomously detects at-risk revenue, diagnoses payment failures with Google Gemini (gemini-3.7-flash), applies deterministic safety and compliance policies, executes bounded recovery actions (such as smart retries and personalized payment link generation), measures recovered revenue, and maintains an immutable audit trail.

---

## Tech Stack

### Backend
- **Language & Framework**: Java 21, Spring Boot 3.4.x
- **Security & Authentication**: Spring Security 6, JJWT (io.jsonwebtoken 0.12.x), BCrypt Password Hashing
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
| `JWT_SECRET` | 256-bit signing key for JWT HMAC-SHA256 tokens | `<your_secure_256_bit_jwt_secret>` |
| `JWT_EXPIRATION_MS` | Access token lifespan in milliseconds | `86400000` (24h) |
| `JWT_ISSUER` | JWT Issuer claim | `RecoverAI` |

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
    webhook:
      signature-header: ${RECOVERY_OUTCOME_WEBHOOK_SIGNATURE_HEADER:X-Recovery-Signature}
```

---

## Recovery Outcome Webhooks & Attempt Reconciliation

RecoverAI provides a secure, multi-tenant webhook ingestion layer for asynchronous communication and payment providers to report outcomes for dispatched `RecoveryAttempt` entities.

### Flow Architecture
```
┌─────────────────────────┐
│     AgentDecision       │
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│RecoveryOrchestratorSvc  │
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│     RecoveryAttempt     │
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│   Provider / Executor   │
└───────────┬─────────────┘
            ▼
 [ Asynchronous Outcome ]
            ▼
┌─────────────────────────┐
│POST /api/v1/webhooks/   │
│    recovery-outcome     │
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│HMAC-SHA256 Verification │
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│  Durable Idempotency    │
│(recovery_outcome_events)│
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│  State Machine Valid.   │
│(RecoveryAttemptStateMch)│
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│Reconcile RecoveryAttempt│
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ Reconcile RecoveryCase  │
│ (RECOVERED on SUCCESS)  │
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│       AuditEvent        │
└─────────────────────────┘
```

### Endpoint Specification
- **URL**: `POST /api/v1/webhooks/recovery-outcome`
- **Content-Type**: `application/json`
- **Headers**:
  - `X-Recovery-Signature: <hmac_sha256_hex>` (or `X-Webhook-Signature`)

#### Sample Request Payload
```json
{
  "providerEventId": "evt_wa_cb_98765",
  "merchantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "recoveryAttemptId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "outcomeStatus": "SUCCESS",
  "provider": "WHATSAPP",
  "providerReference": "wa_msg_98765",
  "occurredAt": "2026-08-28T18:00:00Z",
  "resultCode": "PAID_VIA_SMART_LINK",
  "resultMessage": "Customer completed payment via smart link",
  "metadata": "{\"paymentMethod\":\"UPI\",\"gatewayPaymentId\":\"pay_test123\"}"
}
```

#### Sample Response Payload
```json
{
  "status": "accepted"
}
```

### State Machine Transition Rules
The `RecoveryAttemptStateMachine` deterministically enforces allowed status lifecycles and forbids backward transitions and mutations to terminal states:
- `SCHEDULED` ➔ `IN_FLIGHT`, `FAILED`, `SKIPPED`
- `IN_FLIGHT` ➔ `SENT`, `DELIVERED`, `CLICKED`, `SUCCESS`, `FAILED`
- `SENT` ➔ `DELIVERED`, `CLICKED`, `SUCCESS`, `FAILED`
- `DELIVERED` ➔ `CLICKED`, `SUCCESS`, `FAILED`
- `CLICKED` ➔ `SUCCESS`, `FAILED`
- Terminal States: `SUCCESS`, `FAILED`, `SKIPPED` (rejects backwards transitions like `SUCCESS` ➔ `IN_FLIGHT`, `FAILED` ➔ `SENT`).

### RecoveryCase Reconciliation & Amount Integrity
- When an attempt transitions to intermediate states (`SENT`, `DELIVERED`, `CLICKED`, `IN_FLIGHT`), `RecoveryCase` remains `IN_PROGRESS`.
- When an attempt transitions to `SUCCESS`:
  - `RecoveryAttempt` status is set to `SUCCESS`, `completedAt` populated.
  - `RecoveryCase` transitions to `RECOVERED`, `recoveredAt` populated.
  - `recoveredAmount` is derived from trusted persistent data (`estimatedRecoverableAmount`), preventing external client/webhook manipulation.
  - Associated `Payment` status is updated to `CAPTURED`.
- When an attempt transitions to `FAILED`:
  - `RecoveryAttempt` status is set to `FAILED`, `completedAt` populated.
  - `RecoveryCase` remains `IN_PROGRESS` (not recovered) so future attempts can be scheduled.

### Multi-Tenant Isolation & Concurrency Safety
1. **Tenant Authorization**: Lookups are strictly merchant-scoped (`findByIdAndMerchantId`). Webhooks signed for Merchant A can never read or mutate Merchant B's attempts, cases, or payments (returns HTTP 404 with safe error details).
2. **Durable Idempotency**: Persistent `recovery_outcome_events` table (Flyway V4 migration) with unique constraint `(merchant_id, provider, provider_event_id)`.
3. **Concurrent Duplicate Protection**: If two identical webhook deliveries arrive concurrently, the database unique constraint prevents duplicate processing; subsequent deliveries return HTTP 200 `{ "status": "accepted" }` and emit `RECOVERY_OUTCOME_DUPLICATE` audit events without duplicate mutations.

### Audit Events
Structured audit logging via `AuditService` (`ActorType.WEBHOOK`):
- `RECOVERY_OUTCOME_RECEIVED`
- `RECOVERY_OUTCOME_PROCESSED`
- `RECOVERY_OUTCOME_DUPLICATE`
- `RECOVERY_OUTCOME_REJECTED`
- `RECOVERY_ATTEMPT_STATUS_UPDATED`
- `RECOVERY_ATTEMPT_SUCCEEDED`
- `RECOVERY_ATTEMPT_FAILED`

---

---

## Recovery Scheduling & Automated Background Poller

RecoverAI provides an automated and resilient Recovery Scheduling system (`RecoverySchedulerService`, `RecoverySchedulerWorker`) that allows recovery attempts to be planned for future execution or queued for immediate background processing.

### Architecture & Polling Lifecycle
```
┌────────────────────────────────────────────────────────┐
│  POST /api/v1/recovery-cases/{id}/schedule             │
│  Payload: { "scheduledAt": "2026-08-28T19:30:00Z" }    │
└─────────────────────────┬──────────────────────────────┘
                          │
                          ▼
┌────────────────────────────────────────────────────────┐
│  RecoverySchedulerService.scheduleRecovery             │
│  - Multi-tenant ownership check                        │
│  - Guard against terminal cases (RECOVERED, etc.)      │
│  - Idempotency check for active attempts               │
│  - Sequential attempt numbering                        │
│  - Case state: OPEN ➔ IN_PROGRESS                      │
│  - Persist RecoveryAttempt in SCHEDULED status         │
└────────────────────────────────────────────────────────┘
                          │
                          ▼
┌────────────────────────────────────────────────────────┐
│  Periodic Background Worker (@Scheduled)               │
│  RecoverySchedulerWorker (Fixed delay: 5000ms)         │
└─────────────────────────┬──────────────────────────────┘
                          │
                          ▼
┌────────────────────────────────────────────────────────┐
│  RecoverySchedulerService.pollAndExecuteDueAttempts    │
│  - Queries DB for due SCHEDULED attempts (idx_status_  │
│    scheduled_at composite index)                       │
│  - Atomic claim: SCHEDULED ➔ IN_FLIGHT                 │
│  - Prevents race conditions across clustered nodes     │
│  - Terminal case guard (skips if case already solved)  │
│  - Dispatches to RecoveryActionExecutor                │
│  - Reconciles case to RECOVERED on immediate success   │
│  - Emits immutable AuditEvents                         │
└────────────────────────────────────────────────────────┘
```

### Endpoints Specification
- **URL**: `POST /api/v1/recovery-cases/{recoveryCaseId}/schedule`
  - **Header**: `X-Merchant-Id: <uuid>`
- **Alternative URL**: `POST /api/v1/merchants/{merchantId}/recovery-cases/{recoveryCaseId}/schedule`

#### Request Payload
```json
{
  "scheduledAt": "2026-08-28T19:30:00Z"
}
```
*Note: `scheduledAt` is optional. If omitted or null, it defaults to the current timestamp (`Instant.now()`).*

#### Response Payload (201 Created)
```json
{
  "id": "uuid",
  "recoveryCaseId": "uuid",
  "merchantId": "uuid",
  "attemptNumber": 1,
  "channel": "WHATSAPP",
  "status": "SCHEDULED",
  "scheduledAt": "2026-08-28T19:30:00Z",
  "executedAt": null,
  "completedAt": null,
  "resultCode": null,
  "resultMessage": null,
  "recoveryLink": null,
  "metadata": "{\"agentDecisionId\":\"...\",\"recommendedAction\":\"WHATSAPP_SMART_LINK\"}",
  "createdAt": "2026-08-28T18:00:00Z",
  "updatedAt": "2026-08-28T18:00:00Z"
}
```

### Concurrency, Idempotency & Clustering Strategy
1. **Composite Index Polling**: Flyway migration `V5__create_recovery_scheduler_index.sql` adds a composite index on `recovery_attempts(status, scheduled_at)` to ensure high-performance polling under heavy volumes.
2. **Atomic DB-Level Claiming**: Claiming is executed via an atomic update query:
   ```sql
   UPDATE recovery_attempts
   SET status = 'IN_FLIGHT', executed_at = :now, updated_at = :now
   WHERE id = :id AND status = 'SCHEDULED'
   ```
   In a clustered multi-node deployment, only the single worker that receives `rowsUpdated == 1` proceeds with execution. All competing workers receive `0` and safely yield, guaranteeing zero duplicate dispatches.
3. **Isolated Subtransactions**: Execution for each attempt is isolated in a separate transaction (`Propagation.REQUIRES_NEW`), ensuring an unexpected failure during one attempt's execution never aborts the overall polling batch or other attempts.
4. **Terminal Case Guarding**: If a recovery case transitions to a terminal state (`RECOVERED`, `CANCELLED`, `EXPIRED`) while waiting in `SCHEDULED` status, the claiming service detects this prior to execution, marks the attempt `SKIPPED` with result code `CASE_TERMINAL`, and prevents unnecessary customer communication or payment retries.
5. **Multi-Tenant Security**: Strict merchant ownership is verified during scheduling (`findByIdAndMerchantId`). Attempt entities inherit the merchant identifier and all audit events are scoped to the merchant.

### Configuration Properties
| Property | Environment Variable | Default | Description |
| :--- | :--- | :--- | :--- |
| `recoverai.recovery.scheduler.enabled` | `RECOVERY_SCHEDULER_ENABLED` | `true` | Enables or disables background polling worker |
| `recoverai.recovery.scheduler.polling-interval-ms` | `RECOVERY_SCHEDULER_POLLING_INTERVAL_MS` | `5000` | Polling cycle delay in milliseconds |
| `recoverai.recovery.scheduler.batch-size` | `RECOVERY_SCHEDULER_BATCH_SIZE` | `50` | Maximum number of due attempts claimed per cycle |

---

## Merchant Authentication & JWT Security

RecoverAI features a production-grade merchant authentication and authorization subsystem built on **Spring Security 6**, **JJWT (io.jsonwebtoken 0.12.x)**, and **BCrypt password hashing**. It replaces untrusted `X-Merchant-Id` header authentication with cryptographically signed JSON Web Tokens (JWT) while strictly preserving multi-tenant isolation.

```
                    ┌─────────────────────────┐
                    │ Merchant Client / UI    │
                    └───────────┬─────────────┘
                                │
        1. POST /api/v1/auth/login (email + password)
                                │
                                ▼
                    ┌─────────────────────────┐
                    │       AuthService       │
                    │ - BCrypt match password │
                    │ - Issue signed JWT      │
                    └───────────┬─────────────┘
                                │
              2. Returns Bearer Access Token (JWT)
                                │
                                ▼
   3. Requests with `Authorization: Bearer <jwt>`
                                │
                                ▼
                    ┌─────────────────────────┐
                    │ JwtAuthenticationFilter │
                    │ - Verify HMAC signature │
                    │ - Verify expiration     │
                    │ - Set MerchantPrincipal │
                    └───────────┬─────────────┘
                                │
                                ▼
         ┌──────────────────────────────────────────────────┐
         │ Multi-Tenant Protected Business Endpoints        │
         │ - Auth merchant ID from JWT is authoritative     │
         │ - Header/path spoofing rejected (403 Forbidden)  │
         │ - Repository queries scoped to merchant          │
         └──────────────────────────────────────────────────┘
```

### Endpoints Specification

#### 1. Merchant Registration
- **URL**: `POST /api/v1/auth/register`
- **Access**: Public

##### Request Body
```json
{
  "name": "Acme Retailers Pvt Ltd",
  "email": "payments@acmeretail.com",
  "password": "StrongPassword123!",
  "razorpayAccountId": "acc_123456789",
  "webhookSecret": "whsec_customsecret"
}
```
*Note: `razorpayAccountId` and `webhookSecret` are optional. If `webhookSecret` is omitted, a cryptographically secure secret is generated automatically.*

##### Response Body (`201 Created`)
```json
{
  "id": "78328114-68f4-411a-85b4-d5ebdb932371",
  "name": "Acme Retailers Pvt Ltd",
  "email": "payments@acmeretail.com",
  "razorpayAccountId": "acc_123456789",
  "status": "ACTIVE",
  "createdAt": "2026-08-28T18:00:00Z",
  "updatedAt": "2026-08-28T18:00:00Z"
}
```
*Sensitive fields like `passwordHash` and `webhookSecret` are never exposed in response DTOs.*

---

#### 2. Merchant Login
- **URL**: `POST /api/v1/auth/login`
- **Access**: Public

##### Request Body
```json
{
  "email": "payments@acmeretail.com",
  "password": "StrongPassword123!"
}
```

##### Response Body (`200 OK`)
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJSZWNvdmVyQUkiLCJzdWIiOiI3ODMyODExNC02OGY0LTQxMWEtODViNC1kNWViZGI5MzIzNzEiLCJtZXJjaGFudElkIjoiNzgzMjgxMTQtNjhmNC00MTFhLTg1YjQtZDVlYmRiOTMyMzcxIiwiZW1haWwiOiJwYXltZW50c0BhY21lcmV0YWlsLmNvbSIsIm5hbWUiOiJBY21lIFJldGFpbGVycyBQdnQgTHRkIiwicm9sZSI6IlJPTEVfTUVSQ0hBTlQiLCJpYXQiOjE3NTY0MDY0MDAsImV4cCI6MTc1NjQ5MjgwMH0.signature",
  "tokenType": "Bearer",
  "expiresInMs": 86400000,
  "merchant": {
    "id": "78328114-68f4-411a-85b4-d5ebdb932371",
    "name": "Acme Retailers Pvt Ltd",
    "email": "payments@acmeretail.com",
    "razorpayAccountId": "acc_123456789",
    "status": "ACTIVE",
    "createdAt": "2026-08-28T18:00:00Z",
    "updatedAt": "2026-08-28T18:00:00Z"
  }
}
```

---

### Protected APIs & Authorization Header Format

All business APIs require the JWT token to be supplied in the standard HTTP header:
```http
Authorization: Bearer <jwt-token>
```

| Endpoint Path | Method | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/auth/register` | `POST` | Public | Merchant registration |
| `/api/v1/auth/login` | `POST` | Public | Merchant authentication & JWT issuance |
| `/api/v1/health` | `GET` | Public | Application health check |
| `/actuator/health` | `GET` | Public | Spring Boot Actuator health probe |
| `/api/v1/webhooks/razorpay` | `POST` | Public (HMAC Verified) | Razorpay payment failure ingestion (via `X-Razorpay-Signature`) |
| `/api/v1/webhooks/recovery-outcome` | `POST` | Public (HMAC Verified) | Recovery outcome callback (via `X-Recovery-Signature`) |
| `/api/v1/recovery-cases/{id}/diagnose` | `POST` | Authenticated | Trigger AI diagnosis for a case |
| `/api/v1/recovery-cases/{id}/orchestrate` | `POST` | Authenticated | Trigger immediate recovery execution |
| `/api/v1/recovery-cases/{id}/schedule` | `POST` | Authenticated | Schedule a recovery attempt |

---

### Security Architecture & Design Decisions

1. **Deterministic Multi-Tenant Scoping**:
   - The authenticated merchant ID extracted from the validated JWT token is the **authoritative identity**.
   - If a client provides an explicit `X-Merchant-Id` header or path variable `/merchants/{merchantId}/...` that belongs to a *different* merchant, the system rejects the request immediately with **403 Forbidden** (`TenantMismatchException`).
   - Repository queries remain strictly tenant-scoped (e.g. `findByIdAndMerchantId`).
2. **Password Security**:
   - Passwords are encrypted using Spring Security's `BCryptPasswordEncoder` with a secure cost factor.
   - Plaintext passwords are never stored in the database or logged in application logs.
   - Salting is handled automatically and uniquely per hash.
3. **JWT Cryptographic Integrity**:
   - Signed using HMAC-SHA256 with a 256-bit secret key.
   - Token payload includes `sub` (Merchant ID), `merchantId`, `email`, `name`, `role`, `iat`, `exp`, and `iss`.
   - Tampered, expired, or malformed tokens are intercepted by `JwtAuthenticationFilter` and rejected with `401 Unauthorized`.
4. **Webhook Security Distinction**:
   - Webhook endpoints (`/api/v1/webhooks/**`) remain open to external automated callers (Razorpay gateway and recovery delivery providers) without requiring merchant JWT access tokens.
   - Webhooks are protected via constant-time HMAC-SHA256 signature verification (`X-Razorpay-Signature`, `X-Recovery-Signature`) against each merchant's secret.
5. **Standardized Error Handling**:
   - Authentication errors (`InvalidCredentialsException`, `AuthenticationException`) return standardized JSON `ApiErrorResponse` (`401 Unauthorized`).
   - Authorization failures return `403 Forbidden`.
   - Duplicate registrations return `409 Conflict`.
   - Internal credentials, secrets, and stack traces are never exposed in error responses.

---

### JWT Security Configuration Properties
| Property | Environment Variable | Default | Description |
| :--- | :--- | :--- | :--- |
| `recoverai.security.jwt.secret` | `JWT_SECRET` | *Configurable secret* | 256-bit secret key for HMAC-SHA256 signing |
| `recoverai.security.jwt.expiration-ms` | `JWT_EXPIRATION_MS` | `86400000` (24 hours) | JWT access token validity duration in ms |
| `recoverai.security.jwt.issuer` | `JWT_ISSUER` | `RecoverAI` | Issuer claim embedded in generated tokens |

---

---

## Merchant Dashboard & Recovery Management APIs

RecoverAI provides merchant-scoped recovery case management and dashboard metrics.

### Endpoints Specification

| Endpoint Path | Method | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/dashboard/summary` | `GET` | Authenticated | High-level merchant summary metrics |
| `/api/v1/recovery-cases` | `GET` | Authenticated | Paginated and filtered list of merchant recovery cases |
| `/api/v1/recovery-cases/{id}` | `GET` | Authenticated | Detailed recovery case information with attempts and AI diagnosis |
| `/api/v1/recovery-cases/{id}/attempts` | `GET` | Authenticated | Ordered history of recovery attempts for a case |
| `/api/v1/recovery-cases/{id}/cancel` | `PATCH` | Authenticated | Cancel an open or in-progress recovery case and skip scheduled attempts |

---

## Recovery Analytics & Reporting Engine

RecoverAI features a production-ready, merchant-scoped recovery analytics and reporting engine providing deep insights into recovery metrics, daily performance trends, payment failure reasons, communication channel efficacy, and recovery attempt breakdowns.

### Analytics Endpoints

| Endpoint Path | Method | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/analytics/overview` | `GET` | Authenticated | Comprehensive recovery KPIs and average recovery time |
| `/api/v1/analytics/recovery-trends` | `GET` | Authenticated | Daily periodic trends of cases, at-risk volume, recovered amounts, and recovery rate |
| `/api/v1/analytics/failures` | `GET` | Authenticated | Revenue risk & recovery analytics grouped by failure category and priority |
| `/api/v1/analytics/channels` | `GET` | Authenticated | Conversion, delivery, and success metrics grouped by recovery channel |
| `/api/v1/analytics/attempts` | `GET` | Authenticated | Status breakdown, channel distribution, and average attempts per recovery case |

### Common Request Parameters

All analytics endpoints support optional date-range filtering:

| Parameter | Type | In | Description | Default |
| :--- | :--- | :--- | :--- | :--- |
| `from` | `String` | Query | Start date/time (ISO-8601 format: `YYYY-MM-DD` or `YYYY-MM-DDTHH:mm:ssZ`) | 30 days prior to `to` |
| `to` | `String` | Query | End date/time (ISO-8601 format: `YYYY-MM-DD` or `YYYY-MM-DDTHH:mm:ssZ`) | Current timestamp (`now`) |
| `Authorization` | `String` | Header | Bearer JWT access token (`Bearer <jwt>`) | **Required** |
| `X-Merchant-Id` | `UUID` | Header | Optional explicit merchant ID (validated against JWT token) | Authenticated Merchant ID |

### Date Range Validation Rules
1. **Sensible Defaults**: If both `from` and `to` are omitted, a 30-day window (`[now - 30 days, now]`) is applied automatically.
2. **Flexible Formats**: Accepts both ISO-8601 date strings (`2026-08-01`) and full UTC timestamps (`2026-08-01T00:00:00Z`).
3. **Range Constraints**:
   - `from` must be before or equal to `to`. If `from > to`, returns **HTTP 400 Bad Request**.
   - Maximum date span cannot exceed **365 days** to prevent expensive unbounded queries.
   - Invalid formats return **HTTP 400 Bad Request** with a descriptive `ApiErrorResponse`.

---

### Endpoint Details & Response Structures

#### 1. Recovery Analytics Overview
- **URL**: `GET /api/v1/analytics/overview`
- **Description**: Returns top-level KPIs including case distributions, monetary totals, recovery rate, average recovered amount, and average time to recovery.

##### Sample Response (`200 OK`)
```json
{
  "totalCases": 50,
  "openCases": 10,
  "inProgressCases": 15,
  "recoveredCases": 20,
  "failedCases": 2,
  "expiredCases": 2,
  "cancelledCases": 1,
  "expiredOrCancelledCases": 3,
  "totalEstimatedRecoverableAmount": 150000.00,
  "totalRecoveredAmount": 65000.00,
  "recoveryRate": 40.00,
  "averageRecoveredAmount": 3250.00,
  "averageTimeToRecoverySeconds": 4320.00,
  "from": "2026-07-29T14:00:00Z",
  "to": "2026-08-28T14:00:00Z"
}
```

---

#### 2. Recovery Trend Analytics
- **URL**: `GET /api/v1/analytics/recovery-trends`
- **Description**: Returns daily recovery volume, at-risk capital, recovered amounts, and daily recovery rate aggregated at the database level with deterministic ascending date ordering.

##### Sample Response (`200 OK`)
```json
{
  "from": "2026-08-01T00:00:00Z",
  "to": "2026-08-28T23:59:59.999Z",
  "totalCases": 25,
  "totalAmountAtRisk": 75000.00,
  "totalRecoveredAmount": 35000.00,
  "overallRecoveryRate": 46.67,
  "trends": [
    {
      "date": "2026-08-01",
      "recoveryCasesCreated": 5,
      "amountAtRisk": 15000.00,
      "amountRecovered": 7500.00,
      "recoveredCaseCount": 3,
      "recoveryRate": 60.00
    },
    {
      "date": "2026-08-02",
      "recoveryCasesCreated": 8,
      "amountAtRisk": 24000.00,
      "amountRecovered": 12000.00,
      "recoveredCaseCount": 4,
      "recoveryRate": 50.00
    }
  ]
}
```

---

#### 3. Failure Analytics
- **URL**: `GET /api/v1/analytics/failures`
- **Description**: Groups payment failures by `failureReasonCategory` and `priority` to help merchants identify which root causes represent the highest revenue risk.

##### Sample Response (`200 OK`)
```json
{
  "from": "2026-07-29T14:00:00Z",
  "to": "2026-08-28T14:00:00Z",
  "totalCases": 40,
  "categories": [
    {
      "failureReasonCategory": "INSUFFICIENT_FUNDS",
      "caseCount": 20,
      "estimatedRecoverableAmount": 60000.00,
      "recoveredAmount": 30000.00,
      "recoveredCaseCount": 10,
      "recoveryRate": 50.00
    },
    {
      "failureReasonCategory": "AUTHENTICATION_ERROR",
      "caseCount": 15,
      "estimatedRecoverableAmount": 45000.00,
      "recoveredAmount": 22500.00,
      "recoveredCaseCount": 8,
      "recoveryRate": 53.33
    }
  ],
  "priorities": [
    {
      "priority": "CRITICAL",
      "caseCount": 10,
      "estimatedRecoverableAmount": 50000.00,
      "recoveredAmount": 35000.00,
      "recoveredCaseCount": 7,
      "recoveryRate": 70.00
    },
    {
      "priority": "HIGH",
      "caseCount": 20,
      "estimatedRecoverableAmount": 40000.00,
      "recoveredAmount": 15000.00,
      "recoveredCaseCount": 8,
      "recoveryRate": 40.00
    }
  ]
}
```

---

#### 4. Channel Performance Analytics
- **URL**: `GET /api/v1/analytics/channels`
- **Description**: Analyzes recovery attempts per communication channel (`WHATSAPP`, `EMAIL`, `SMS`, `RETRY_CHARGE`, `SMART_LINK`, `MANUAL`) measuring total dispatches, deliveries, clicks, success rate, and attributable recovered revenue.

##### Sample Response (`200 OK`)
```json
{
  "from": "2026-07-29T14:00:00Z",
  "to": "2026-08-28T14:00:00Z",
  "totalAttempts": 80,
  "channels": [
    {
      "channel": "WHATSAPP",
      "totalAttempts": 45,
      "successfulAttempts": 25,
      "failedAttempts": 5,
      "sentAttempts": 10,
      "deliveredAttempts": 5,
      "clickedAttempts": 0,
      "successRate": 55.56,
      "recoveredAmount": 52000.00
    },
    {
      "channel": "EMAIL",
      "totalAttempts": 25,
      "successfulAttempts": 8,
      "failedAttempts": 7,
      "sentAttempts": 8,
      "deliveredAttempts": 2,
      "clickedAttempts": 0,
      "successRate": 32.00,
      "recoveredAmount": 18000.00
    },
    {
      "channel": "RETRY_CHARGE",
      "totalAttempts": 10,
      "successfulAttempts": 6,
      "failedAttempts": 4,
      "sentAttempts": 0,
      "deliveredAttempts": 0,
      "clickedAttempts": 0,
      "successRate": 60.00,
      "recoveredAmount": 12000.00
    }
  ]
}
```

---

#### 5. Recovery Attempt Analytics
- **URL**: `GET /api/v1/analytics/attempts`
- **Description**: Comprehensive breakdown of recovery attempts by lifecycle status and channel, including calculation of average attempts per recovery case.

##### Sample Response (`200 OK`)
```json
{
  "from": "2026-07-29T14:00:00Z",
  "to": "2026-08-28T14:00:00Z",
  "totalAttempts": 80,
  "successfulAttempts": 39,
  "failedAttempts": 16,
  "scheduledAttempts": 10,
  "inFlightAttempts": 3,
  "sentAttempts": 8,
  "deliveredAttempts": 4,
  "clickedAttempts": 0,
  "skippedAttempts": 0,
  "successRate": 48.75,
  "averageAttemptsPerRecoveryCase": 1.60,
  "attemptsByStatus": {
    "SCHEDULED": 10,
    "IN_FLIGHT": 3,
    "SENT": 8,
    "DELIVERED": 4,
    "CLICKED": 0,
    "SUCCESS": 39,
    "FAILED": 16,
    "SKIPPED": 0
  },
  "attemptsByChannel": {
    "WHATSAPP": 45,
    "EMAIL": 25,
    "SMS": 0,
    "RETRY_CHARGE": 10,
    "SMART_LINK": 0,
    "MANUAL": 0
  }
}
```

---

### Security & Multi-Tenant Isolation Guarantees

1. **JWT Authentication Required**: All analytics endpoints require a valid JWT bearer token. Unauthenticated requests are rejected with **HTTP 401 Unauthorized**.
2. **Tenant Scoping from Security Context**: Merchant identity is always resolved authoritatively from `SecurityUtils.getCurrentMerchantId()`.
3. **Anti-Spoofing Protection**: Any mismatch between the authenticated merchant and supplied headers/parameters immediately yields **HTTP 403 Forbidden**.
4. **Database-Level Isolation**: Every SQL/JPQL aggregation strictly enforces `WHERE rc.merchant.id = :merchantId` or `WHERE ra.merchant.id = :merchantId`.
5. **High-Performance Aggregations**: All analytics metrics are computed via database-level `SUM`, `COUNT`, `AVG`, and `GROUP BY` projections without loading entire entity graphs into memory. Composite indexes (Flyway V7 & V8) accelerate multi-tenant analytics filtering.

---

---

## Recovery Strategy Engine

The **Deterministic Recovery Strategy Engine** (`RecoveryStrategyEngine`, `RecoveryStrategyService`, `RecoveryStrategyController`) consumes the raw AI diagnosis (`AgentDecision`), payment failure telemetry, previous attempt history, customer contact availability, and configurable business policies to produce a deterministic, safe, and auditable **Recovery Strategy** before any attempt execution or scheduling.

```
+---------------------+
|   AI Diagnosis      |  Produces probabilistic AgentDecision
| (Gemini 3.7 Flash)  |  (recommendedAction, channel, confidenceScore)
+----------+----------+
           |
           v
+---------------------+
|  Recovery Strategy  |  Deterministic Policy Enforcement:
|       Engine        |  - Terminal Case Protection (RECOVERED/CANCELLED/EXPIRED)
|                     |  - High/Low Confidence Thresholding
+----------+----------+  - Failure Category Eligibility (insufficient_funds, network_error vs auth/invalid)
           |             - Customer Contact Channel Viability (Phone/Email availability)
           |             - Previous Attempt & Failure History / Fallback Cascading
           |             - Configurable Max Attempts Limit Guard
           v
+---------------------+
|  RecoveryStrategy   |  Persisted in PostgreSQL (Flyway V9 `recovery_strategies`)
|     (Persisted)     |  Immutable audit record with chosen channel, priority, delay, fallback
+----------+----------+
           |
           +----------------------------------+
           |                                  |
           v                                  v
+-----------------------+          +----------------------+
| Recovery Orchestrator |          |  Recovery Scheduler  |
|      Service          |          |       Service        |
+----------+------------+          +----------+-----------+
           |                                  |
           +-----------------+----------------+
                             |
                             v
               +---------------------------+
               |  RecoveryActionExecutors  |
               | (WhatsApp, Email, SMS,    |
               |  SmartLink, RetryCharge)  |
               +---------------------------+
```

### Deterministic Strategy Policy Rules

1. **Terminal Case Guarding**:
   - If a recovery case is already `RECOVERED`, `CANCELLED`, or `EXPIRED`, or its underlying payment is `CAPTURED` or `REFUNDED`, the engine outputs a terminal strategy (`isTerminal = true`, action `NO_ACTION_TERMINAL`).
   - Prevents any further scheduling or execution on completed cases.

2. **Maximum Attempts Enforcement**:
   - Compares total prior recovery attempts against `recoverai.recovery.strategy.max-attempts` (default: 3).
   - If the limit is reached, a terminal strategy is generated (`isTerminal = true`, action `MAX_ATTEMPTS_EXCEEDED`), preventing duplicate or infinite attempt storms.

3. **AI Confidence Thresholding**:
   - Evaluates `AgentDecision.confidenceScore` against `recoverai.recovery.strategy.min-ai-confidence` (default: `0.70`).
   - **Low Confidence (< 0.70)**: Automatically avoids automated payment re-charges (`RETRY_CHARGE`) and switches to conservative customer communication channels (`WHATSAPP` -> `EMAIL` -> `SMS` -> `SMART_LINK` -> `MANUAL`).

4. **Payment Retry Charge Eligibility**:
   - `RETRY_CHARGE` is only permitted when:
     - `retry-charge-enabled` is `true`.
     - AI confidence is sufficiently high (`>= 0.70`).
     - Payment failure category is technically recoverable (e.g. `insufficient_funds`, `network_error`, `system_error`, `temporary_technical_issue`, `timeout`).
     - Permanent/fatal failure categories (e.g. `authentication_failure`, `card_expired`, `invalid_request`, `bank_declined`, `fraud_suspected`) strictly prohibit automated re-charging.
     - `RETRY_CHARGE` has not already been attempted previously for this case (capped at 1 retry charge attempt).

5. **Customer Contact Availability & Viability**:
   - `WHATSAPP` and `SMS`: Require a valid customer phone number (`customer.phone`).
   - `EMAIL`: Requires a valid customer email address (`customer.email`).
   - `SMART_LINK`: Requires either phone or email for link delivery.
   - `MANUAL`: Fallback when no automated communication channels or contact information are present.

6. **Channel Failure Cascading & Fallbacks**:
   - Tracks failed attempts per channel. Channels exceeding `max-channel-failures` (default: 1) are marked exhausted.
   - Fallback hierarchy: `WHATSAPP` -> `EMAIL` -> `SMS` -> `SMART_LINK` -> `MANUAL`.
   - Strategies persist designated `fallbackChannel` and `fallbackAction` for downstream recovery resilience.

---

### Configuration Properties

| Property | Environment Variable | Default | Description |
| :--- | :--- | :--- | :--- |
| `recoverai.recovery.strategy.enabled` | `RECOVERY_STRATEGY_ENABLED` | `true` | Enable/disable deterministic strategy engine |
| `recoverai.recovery.strategy.min-ai-confidence` | `RECOVERY_STRATEGY_MIN_AI_CONFIDENCE` | `0.70` | Minimum confidence score required for aggressive strategies |
| `recoverai.recovery.strategy.max-attempts` | `RECOVERY_STRATEGY_MAX_ATTEMPTS` | `3` | Maximum allowed recovery attempts per case |
| `recoverai.recovery.strategy.retry-charge-enabled` | `RECOVERY_STRATEGY_RETRY_CHARGE_ENABLED` | `true` | Allow automated payment re-charging when eligible |
| `recoverai.recovery.strategy.fallback-enabled` | `RECOVERY_STRATEGY_FALLBACK_ENABLED` | `true` | Enable automatic channel fallback calculation |
| `recoverai.recovery.strategy.max-channel-failures` | `RECOVERY_STRATEGY_MAX_CHANNEL_FAILURES` | `1` | Max failures on a single channel before switching |
| `recoverai.recovery.strategy.default-delay-seconds` | `RECOVERY_STRATEGY_DEFAULT_DELAY_SECONDS` | `0` | Default execution delay for communication channels |
| `recoverai.recovery.strategy.retry-delay-seconds` | `RECOVERY_STRATEGY_RETRY_DELAY_SECONDS` | `300` | Execution delay (seconds) for payment retry charges |

---

### Strategy API Endpoints

#### 1. Generate Strategy
- **URL**: `POST /api/v1/recovery-cases/{id}/strategy` (or `POST /api/v1/merchants/{merchantId}/recovery-cases/{id}/strategy`)
- **Security**: JWT Bearer Token required
- **Description**: Evaluates the recovery case and persists a new `RecoveryStrategy`.

##### Sample Response (`200 OK`)
```json
{
  "id": "e4b1b34a-939a-4c28-98e2-9d32bb58231a",
  "recoveryCaseId": "2ba82602-6028-47f7-ade2-12a0ba189736",
  "merchantId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
  "channel": "RETRY_CHARGE",
  "recommendedAction": "RETRY_CHARGE",
  "priority": "HIGH",
  "delaySeconds": 300,
  "maxAttempts": 3,
  "confidenceScore": 0.8800,
  "reason": "AI recommended RETRY_CHARGE with high confidence (0.8800) and retry eligibility satisfied.",
  "fallbackChannel": "WHATSAPP",
  "fallbackAction": "SEND_WHATSAPP_REMINDER",
  "terminal": false,
  "createdAt": "2026-08-28T14:30:00Z"
}
```

#### 2. Get Latest Strategy
- **URL**: `GET /api/v1/recovery-cases/{id}/strategy` (or `GET /api/v1/merchants/{merchantId}/recovery-cases/{id}/strategy`)
- **Security**: JWT Bearer Token required
- **Description**: Retrieves the most recently computed and persisted recovery strategy for the specified recovery case.

---

### Audit Logging Events

All strategy evaluations are recorded to the immutable `audit_events` log with `ActorType.SYSTEM` and `actorId = "RecoveryStrategyEngine"`:
- `RECOVERY_STRATEGY_GENERATED`: Successfully created actionable strategy.
- `RECOVERY_STRATEGY_FALLBACK`: Fallback channel selected due to contact/failure constraints.
- `RECOVERY_STRATEGY_TERMINAL`: Terminal case or max attempts limit encountered.
- `RECOVERY_STRATEGY_REJECTED`: Unviable strategy or execution prevented.

---

## Current Project Status

- **Implemented Features**:
  - `feature/project-foundation`: Spring Boot 3.4.x + Java 21 foundation, Actuator health, CORS, Vite + React shell.
  - `feature/database-schema`: Core domain entities (Merchant, Customer, Payment, RecoveryCase, RecoveryAttempt, AgentDecision, AuditEvent) with Flyway V1 and V2 migrations.
  - `feature/razorpay-payment-ingestion`: Robust Razorpay webhook ingestion endpoint (`POST /api/v1/webhooks/razorpay`), HMAC-SHA256 constant-time verification, multi-tenant merchant resolution, customer/payment upserting, Flyway V3 `webhook_events` idempotency tracking, deterministic failure categorization, RecoveryCase generation, and audit logging.
  - `feature/ai-failure-diagnosis-engine`: Google Gemini AI failure diagnosis engine (`AIDiagnosisService`, `GeminiClient`, `AIDiagnosisController`), structured JSON recommendations, confidence score validation, tenant isolation, PII masking, `AgentDecision` persistence.
  - `feature/recovery-orchestration`: Recovery Orchestration layer (`RecoveryOrchestratorService`, `RecoveryActionExecutor`, `DefaultRecoveryActionExecutor`, `RecoveryOrchestrationController`), lifecycle state machine, DB-backed attempt sequencing, duplicate protection, multi-tenant scoping, and audit logging.
  - `feature/recovery-communication`: Recovery Communication & Execution Layer (`WhatsAppRecoveryExecutor`, `EmailRecoveryExecutor`, `SmsRecoveryExecutor`, `SmartLinkRecoveryExecutor`, `RetryChargeRecoveryExecutor`, `ManualRecoveryExecutor`, `DefaultRecoveryLinkService`, safe mock providers, configuration properties).
  - `feature/recovery-outcome-webhooks`: Recovery Outcome Webhook & Attempt Reconciliation Layer (`POST /api/v1/webhooks/recovery-outcome`, `RecoveryOutcomeService`, `RecoveryAttemptStateMachine`, `RecoveryOutcomeSignatureVerifier`, Flyway V4 `recovery_outcome_events` idempotency tracking, trusted case reconciliation, multi-tenant isolation, structured audit trails).
  - `feature/recovery-scheduling`: Automated Recovery Scheduling & Background Poller (`POST /api/v1/recovery-cases/{id}/schedule`, `RecoverySchedulerService`, `RecoverySchedulerWorker`, Flyway V5 index migration, atomic claim concurrency, terminal case guarding).
  - `feature/merchant-authentication`: Complete Merchant Authentication & JWT Security (`POST /api/v1/auth/register`, `POST /api/v1/auth/login`, Spring Security 6 stateless filter chain, JJWT 0.12.x provider, BCrypt password hashing, Flyway V6 `password_hash` migration, tenant identity propagation, tenant spoofing prevention).
  - `feature/merchant-dashboard-api`: Merchant Dashboard & Recovery Case Management API (`GET /api/v1/dashboard/summary`, `GET /api/v1/recovery-cases`, `GET /api/v1/recovery-cases/{id}`, `GET /api/v1/recovery-cases/{id}/attempts`, `PATCH /api/v1/recovery-cases/{id}/cancel`, Flyway V7 indexes, JPA dynamic specifications, multi-tenant scoping).
  - `feature/recovery-analytics`: Comprehensive Recovery Analytics & Reporting Engine (`GET /api/v1/analytics/overview`, `GET /api/v1/analytics/recovery-trends`, `GET /api/v1/analytics/failures`, `GET /api/v1/analytics/channels`, `GET /api/v1/analytics/attempts`, Flyway V8 analytics indexes, database aggregation projections, ISO date-range validation, and automated multi-tenant test suites).
  - `feature/recovery-strategy-engine`: Deterministic Recovery Strategy Engine (`RecoveryStrategyEngine`, `RecoveryStrategyService`, `RecoveryStrategyController`, `RecoveryStrategyRepository`, Flyway V9 `recovery_strategies` migration, strongly-typed `RecoveryStrategyProperties`, deterministic policy evaluation, confidence thresholding, payment retry guard, channel viability & fallback cascading, max-attempt enforcement, tenant isolation, and audit trails).






