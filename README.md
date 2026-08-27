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

## Current Project Status

- **Status**: Foundation & Infrastructure Phase (`feature/project-foundation`)
- **Implemented**:
  - Spring Boot 3.4.x + Java 21 architecture with YAML configuration.
  - PostgreSQL datasource and Flyway migration setup (`V1__init_schema.sql`).
  - Spring Boot Actuator and custom `/api/v1/health` endpoints.
  - Environment-based CORS configuration.
  - React + Vite + TypeScript frontend shell with Tailwind CSS v4 and Lucide icons.
  - Automated test suite for backend context, health check APIs, and Flyway migration.
  - Docker Compose configuration for local PostgreSQL development.
