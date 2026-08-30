# RecoverAI Merchant Portal Frontend

Professional B2B SaaS frontend for RecoverAI—an autonomous, safe revenue recovery platform for failed payment transactions (Razorpay Buildathon Track 3).

---

## Tech Stack

- **Framework**: React 19.2.x
- **Build Tool**: Vite 8.2.x
- **Language**: TypeScript (strict mode, erasable syntax)
- **Styling**: Tailwind CSS v4 (`@tailwindcss/vite`)
- **Routing**: React Router v7 (`react-router-dom`)
- **Icons**: Lucide React
- **Linter**: Oxlint
- **Testing**: Vitest + React Testing Library + `@testing-library/jest-dom` + `jsdom`

---

## Directory Structure

```
frontend/src/
├── api/
│   ├── client.ts                 # Centralized HTTP client (fetch, interceptors, 401 handler, base URL)
│   ├── auth.ts                   # Login, registration, and backend health check API calls
│   ├── dashboard.ts              # Dashboard overview metrics API call
│   ├── analytics.ts              # PR #21 Analytics APIs (overview, trends, failures, channels, attempts)
│   ├── recovery-cases.ts         # PR #21 Recovery Cases APIs (list, detail, attempts, cancellation)
│   ├── notifications.ts          # PR #22 Notification APIs (list, get, mark read, mark all read, unread count)
│   ├── notification-preferences.ts # PR #22 Preference APIs (get preferences, update preferences matrix, webhook URL)
│   └── providers.ts              # PR #22 Provider Health telemetry API (actuator health integration)
├── components/
│   ├── layout/
│   │   ├── AppShell.tsx          # Master dashboard shell with responsive sidebar & top navigation
│   │   ├── Sidebar.tsx           # Collapsible navigation drawer with live engine status & active routes
│   │   └── Header.tsx            # Top navigation with live backend health, unread notification badge, & profile dropdown
│   ├── ui/
│   │   ├── Alert.tsx             # Semantic alert banners (info, success, warning, error)
│   │   ├── Avatar.tsx            # Dynamic initials avatar with status indicator
│   │   ├── Badge.tsx             # Status pills with optional live pulse dots
│   │   ├── Button.tsx            # Accessible button variants with built-in loading spinner
│   │   ├── Card.tsx              # Composable card primitives (Header, Title, Content, Footer)
│   │   ├── EmptyState.tsx        # Production empty state with icons and action buttons
│   │   ├── ErrorState.tsx        # User-friendly error recovery container with retry action
│   │   ├── Input.tsx             # Form control with labels, helper text, and accessible error states
│   │   ├── Modal.tsx             # Accessible dialog with focus trap, backdrop, ESC handling, and ARIA roles
│   │   ├── PageHeader.tsx        # Standardized page title, description, and action bar
│   │   ├── Pagination.tsx        # Accessible pagination controls with page indicators and bounds validation
│   │   ├── PasswordInput.tsx     # Secure password input with show/hide eye toggle
│   │   ├── Select.tsx            # Accessible select dropdown matching design system styling
│   │   └── Skeleton.tsx          # Shimmer loading placeholders for cards and metrics
│   ├── analytics/
│   │   ├── DateRangeSelector.tsx # Presets (7d, 30d, 90d, 12m, custom) with client-side bounds validation
│   │   ├── RecoveryTrendChart.tsx# Responsive SVG multi-series chart with dual metric toggle & accessible data table
│   │   ├── ChannelAnalyticsCard.tsx# Breakdown of channel volume, recovery rate, and revenue
│   │   └── FailureAnalyticsCard.tsx# Failure root-causes and priority distribution metrics
│   ├── recovery-cases/
│   │   └── RecoveryTimeline.tsx  # Chronological visual execution timeline of case lifecycle and attempts
│   ├── notifications/
│   │   ├── NotificationItem.tsx  # PR #22 Notification card with unread dot, event badge, timestamps, & quick actions
│   │   └── NotificationDetailModal.tsx # PR #22 Accessible modal displaying case links, payload metadata, & channel deliveries
│   └── settings/
│       ├── NotificationPreferencesMatrix.tsx # PR #22 4x3 Event-Channel toggle matrix, webhook input, & dirty state tracking
│       └── ProviderHealthCard.tsx # PR #22 Operational telemetry dashboard for WhatsApp, Email, SMS, & Payment gateways
├── context/
│   ├── auth-context-def.ts       # Core AuthContext definition
│   ├── AuthContext.tsx           # Authentication session provider & state lifecycle
│   ├── toast-context-def.ts      # Core ToastContext definition
│   └── ToastContext.tsx          # Non-blocking floating toast notifications provider
├── hooks/
│   ├── useAuth.ts                # Hook to access current merchant, token, and session actions
│   └── useToast.ts               # Hook to trigger accessible notifications (success, error, info)
├── pages/
│   ├── auth/
│   │   ├── LoginPage.tsx         # Merchant authentication screen with client validation
│   │   └── RegisterPage.tsx      # Merchant onboarding form matching backend DTO
│   ├── dashboard/
│   │   └── OverviewPage.tsx      # Landing overview with real metrics, pipeline status, and webhook guide
│   ├── analytics/
│   │   └── AnalyticsPage.tsx     # PR #21 Full Analytics overview with trends, channels, and root-causes
│   ├── recovery-cases/
│   │   ├── RecoveryCasesPage.tsx # PR #21 Cases table, multi-parameter filters, and server pagination
│   │   └── RecoveryCaseDetailPage.tsx# PR #21 6-section case detail, AI diagnosis, strategy, attempts, & cancellation
│   ├── notifications/
│   │   └── NotificationsPage.tsx # PR #22 Professional notification center with unread filter, event filter, & pagination
│   ├── settings/
│   │   ├── SettingsLayout.tsx    # PR #22 Settings subnavigation container with tab switcher
│   │   ├── GeneralSettingsPage.tsx # PR #22 Read-only merchant profile, tenant ID, and security protocol details
│   │   ├── NotificationSettingsPage.tsx # PR #22 Notification rules and webhook configuration view
│   │   └── ProviderSettingsPage.tsx # PR #22 Upstream provider status telemetry view
│   └── NotFoundPage.tsx          # 404 fallback page with navigation recovery
├── routes/
│   └── AppRoutes.tsx             # Centralized routing with ProtectedRoute and PublicRoute guards
├── types/
│   ├── api.ts                    # ApiErrorResponse, ApiError class, and error sanitization helper
│   ├── auth.ts                   # Merchant, AuthResponse, LoginRequest, RegisterRequest DTOs
│   ├── dashboard.ts              # DashboardSummary response interface
│   ├── analytics.ts              # PR #21 Analytics DTOs and date range preset types
│   ├── recovery-case.ts          # PR #21 RecoveryCase, Detail, Customer, Payment, AI Diagnosis, Attempt DTOs
│   ├── notifications.ts          # PR #22 Notification, Delivery, Preferences, Filter DTOs
│   └── providers.ts              # PR #22 Provider health telemetry and Actuator response types
├── test/
│   ├── setup.ts                  # Vitest test setup and DOM polyfills
│   ├── apiClient.test.ts         # Bearer header attachment, 401 expiry, and error parser tests
│   ├── analyticsApi.test.ts      # PR #21 Analytics API endpoint and query parameter serialization tests
│   ├── recoveryCasesApi.test.ts  # PR #21 Recovery Cases API endpoint and query parameter tests
│   ├── notificationsApi.test.ts  # PR #22 Notification API client tests (list, get, mark read, mark all read, unread count)
│   ├── notificationPreferencesApi.test.ts # PR #22 Notification preferences API client tests (get, update)
│   ├── providersApi.test.ts      # PR #22 Provider health API client tests (actuator indicator, fallback, mapping)
│   ├── auth.test.tsx             # Login, register, validation, and session expiry UI tests
│   ├── routing.test.tsx          # Route guards, authenticated redirects, and 404 tests
│   ├── components.test.tsx       # Reusable UI component unit tests
│   ├── accessibility.test.tsx    # Semantic labels, ARIA attributes, and keyboard tab navigation tests
│   ├── AnalyticsPage.test.tsx    # PR #21 AnalyticsPage KPI rendering, date range, trends chart, and error states
│   ├── RecoveryCasesPage.test.tsx# PR #21 RecoveryCasesPage table, filters, pagination, and empty states
│   ├── RecoveryCaseDetailPage.test.tsx# PR #21 Detail page sections, AI diagnosis, timeline, and cancellation modal
│   ├── NotificationsPage.test.tsx# PR #22 Notifications center listing, filters, pagination, mark read, & modal tests
│   ├── NotificationPreferencesPage.test.tsx # PR #22 Preference matrix toggles, webhook validation, & dirty-state tests
│   ├── ProviderSettingsPage.test.tsx # PR #22 Provider cards, status states, zero secret exposure, & refresh tests
│   ├── HeaderNotification.test.tsx # PR #22 Header unread notification badge and link navigation tests
│   └── SettingsLayout.test.tsx   # PR #22 Settings tabs subnavigation and account details rendering tests
├── App.tsx                       # Root component wrapping Router, ToastProvider, and AuthProvider
├── index.css                     # Tailwind CSS v4 stylesheet and theme tokens
└── main.tsx                      # Application entry point
```

---

## Route Architecture

| Route | Guard | Description |
| :--- | :--- | :--- |
| `/login` | `PublicRoute` | Merchant login screen. Redirects to `/app` if already authenticated. |
| `/register` | `PublicRoute` | Merchant registration screen. Redirects to `/app` if already authenticated. |
| `/app` | `ProtectedRoute` | Authenticated merchant portal dashboard overview. |
| `/recovery-cases` | `ProtectedRoute` | PR #21 Recovery cases management table with multi-filter toolbar and pagination. |
| `/recovery-cases/:id` | `ProtectedRoute` | PR #21 Comprehensive case detail (Summary, Customer, Payment, AI Diagnosis, Strategy, Timeline, Cancel). |
| `/analytics` | `ProtectedRoute` | PR #21 Recovery analytics, trends chart, channel efficiency, and failure root-cause breakdown. |
| `/notifications` | `ProtectedRoute` | PR #22 Professional notification center with unread filter, event filter, mark-all-read, and delivery detail modal. |
| `/settings` | `ProtectedRoute` | PR #22 Merchant settings portal (redirects to `/settings/general`). |
| `/settings/general` | `ProtectedRoute` | PR #22 Read-only merchant account details, tenant ID, and active JWT session protocol. |
| `/settings/notifications` | `ProtectedRoute` | PR #22 4-event × 3-channel notification preferences matrix with dirty-state tracking and webhook URL config. |
| `/settings/providers` | `ProtectedRoute` | PR #22 Upstream communication (WhatsApp, Email, SMS) and payment provider operational health telemetry. |
| `/` | Redirect | Automatically redirects to `/app` (which routes to `/login` if session is missing). |
| `*` | Catch-all | Accessible 404 Not Found error page with recovery link. |

---

## Authentication & Security Model

1. **Authoritative Backend**: The frontend never trusts client-side state for authorization. The backend Spring Boot security filters remain authoritative.
2. **JWT Storage**: The access token is retained in `localStorage` under `recoverai_token` with the merchant profile in `recoverai_merchant`.
3. **Session Invalidation (401 Interception)**: Any authenticated API call that encounters a `401 Unauthorized` triggers an unauthenticated event in `apiClient`. This automatically clears stored credentials and redirects to `/login` with a safe "Your session has expired" message, preventing infinite redirect loops.
4. **Error Sanitization**: Raw backend stack traces or exceptions (`NullPointerException`, SQL errors) are never exposed. All errors pass through `getHumanReadableErrorMessage()` to provide safe, actionable guidance.
5. **No Secret Leaks**: Webhook secrets, database credentials, and JWT signing keys are never exposed or placed in `VITE_*` public variables.
6. **Cancellation Domain Safeguards**: Cancellation action is strictly guarded according to backend domain rules—only cases in `OPEN`, `IN_PROGRESS`, or `FAILED` states can be cancelled; terminal cases (`RECOVERED`, `EXPIRED`, `CANCELLED`) have cancellation actions disabled and hidden.

---

## PR #23: Interactive Demo Mode Architecture

RecoverAI provides an instantaneous, frictionless "Try Interactive Demo" mode designed for evaluators, judges, and prospects to experience the entire merchant recovery portal without registering an account or entering credentials.

### Key Architectural Principles
1. **Frontend-Only Isolation**: Demo mode operates strictly on the client side via `DemoProvider` (`context/DemoContext.tsx`) and `api/demo.ts`.
2. **Zero Backend Auth Bypass**: Demo mode does NOT create fake JWTs, bypass backend Spring Security filters, or send simulated requests to authenticated production endpoints.
3. **Storage Separation**: Demo state is tracked purely via boolean flag `recoverai_demo_mode` in `localStorage`. The authoritative authentication tokens (`recoverai_token` and `recoverai_merchant`) remain null.
4. **State Transitions**:
   - **Unauthenticated Visitor**: `isAuthenticated = false, isDemoMode = false` -> redirected to `/login`.
   - **Demo Evaluator**: `isAuthenticated = false, isDemoMode = true` -> permitted in `ProtectedRoute`, loads simulated fixtures from `api/demo.ts`.
   - **Authenticated Merchant**: `isAuthenticated = true, isDemoMode = false` -> production merchant session with live Spring Boot backend. Real login automatically exits demo mode.
5. **Entry Experience**: The `/login` page offers a prominent "Try Interactive Demo" CTA card ("No account required • Simulated demo data") positioned adjacent to standard merchant login, preserving keyboard accessibility and tab navigation.
6. **Visual Indicators & Exit Flow**: When active, the application displays a persistent amber `DEMO MODE` badge (`role="status"`) in the top navigation header with an accessible "Exit Demo" action button that clears demo storage and redirects to `/login`.
7. **Simulated Domain Telemetry**: `api/demo.ts` provides realistic, in-memory fixtures for all merchant workflows:
   - Executive Dashboard KPI metrics and recent cases.
   - Recovery Cases management with multi-status filtering, sorting, and pagination.
   - Recovery Case Detail with autonomous Gemini 3.7 Flash diagnosis, fallback strategy timeline, and simulated cancellation.
   - Analytics Overview, recovery trends multi-series chart, channel volume matrix, and failure root-cause distribution.
   - Notification Center with unread badges, multi-channel deliveries modal, and simulated mark-as-read actions.
   - Provider operational telemetry for WhatsApp, Email, SMS, and Payment Gateway.
   - Notification delivery rules matrix and webhook configuration.

---

## PR #24: Complete Interactive Demo Workflow

PR #24 transforms the frontend-only Interactive Demo Mode into a realistic, coherent, and reactive recovery lifecycle that an evaluator can experience end-to-end:

```
Failed Payment → Recovery Case → AI Diagnosis → Recovery Strategy → Recovery Attempt → Customer Recovery → Payment Recovered
```

1. **Central Reactive Demo Store (`src/api/demo.ts`)**:
   - Manages state in-memory with safe `localStorage` cache fallback (`recoverai_demo_store_v1`).
   - Dispatches `recoverai:demo-state-changed` DOM custom events on mutations to reactively notify components (e.g. Header unread count).
   - Dynamic derivation of Dashboard KPIs (`getDemoDashboard()`), Recovery Cases filtering & pagination (`getDemoRecoveryCases()`), and Analytics Overview/Trends/Channels/Failures (`getDemoAnalyticsOverview()`, etc.).
2. **Realistic 10-Case Dataset**:
   - 10 Indian payment failure scenarios in INR (₹) across UPI, Credit/Debit Cards, and Netbanking.
   - Comprehensive status coverage: `OPEN`, `IN_PROGRESS`, `RECOVERED`, `FAILED`, `CANCELLED`, `EXPIRED`.
   - Comprehensive failure reason categories: `AUTHENTICATION`, `INSUFFICIENT_FUNDS`, `NETWORK_TIMEOUT`, `USER_DROPOFF`, `BANK_DECLINED`, `CARD_EXPIRED`.
   - Realistic autonomous Google Gemini 3.7 Flash root-cause deductions, confidence scores, multi-channel strategies, and execution timelines.
3. **Simulate Customer Recovery Action (`src/pages/recovery-cases/RecoveryCaseDetailPage.tsx`)**:
   - In `RecoveryCaseDetailPage`, an evaluator can trigger the interactive recovery simulation for eligible cases (`OPEN` or `IN_PROGRESS`).
   - 4-stage realistic progression: Link accessed → Payment instrument selected → Gateway authorization & capture confirmed → Closed-loop case resolution.
   - Updates payment (`FAILED` → `CAPTURED`), case (`OPEN`/`IN_PROGRESS` → `RECOVERED`), attempt (`DELIVERED`/`SENT` → `SUCCESS`), timestamps, and generates a deduplicated `PAYMENT_RECOVERED` notification.
4. **Terminal Case Protection**:
   - Prevents simulation of already `RECOVERED`, `CANCELLED`, `EXPIRED`, or `FAILED` cases with clear explanatory UI tooltips and badges.
5. **Real-time Navigation & Dashboard Synchronization**:
   - Header unread count badge increments immediately via custom event.
   - Overview Dashboard KPIs (recovered revenue, recovery rate, active cases) recalculate dynamically from the central store without page reloads.
6. **Zero Backend Mutations Guarantee**:
   - 100% frontend-only. Zero network calls to backend database, zero real payment charges, zero SMS/WhatsApp provider dispatches.

---

## PR #25: Professional Fintech SaaS UI Redesign

PR #25 elevates RecoverAI from a functional prototype into a polished, high-trust fintech B2B SaaS platform inspired by industry leaders (Stripe, Razorpay, Linear):

### 1. Light Fintech Color Palette & Design Tokens
- **Background**: Modern, clean slate-50 (`#f8fafc`) page canvas with subtle depth gradients (`bg-radial`, `backdrop-blur`).
- **Primary Surfaces & Cards**: Pure white (`#ffffff`) surfaces, subtle slate borders (`border-slate-200/90`), and refined micro-elevation (`shadow-2xs`, `shadow-xs`).
- **Primary Accent & CTAs**: Crisp emerald green (`bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white focus:ring-emerald-500/20`).
- **Typography & Hierarchy**: Deep slate headings (`#0f172a` / `text-slate-900`), balanced metadata (`text-slate-500` / `text-slate-600`), and tabular figures (`font-mono`) for monetary amounts and transaction IDs.
- **Status Semantics**: Consistent status badges with soft backgrounds and matching borders (`success`: emerald, `warning`: amber, `error`: rose, `info`: sky, `neutral`: slate).

### 2. Modernized UI Primitives (`src/components/ui/`)
- **Button**: 5 refined variants (`primary`, `secondary`, `outline`, `ghost`, `danger`) with emerald focus rings, built-in loading spinners, and icon slots.
- **Card**: Pure white container with micro-shadows, subtle divider borders, and composable header/title/content/footer slots.
- **Badge**: Clean pill badges with rounded corners, optional pulsating live indicator dots, and clear color contrast.
- **Modal**: Accessible floating dialogs with smooth backdrops, focus traps, escape key support, and responsive layouts.
- **Input & PasswordInput**: Slate-bordered form fields with emerald focus halos, clear validation messaging, and accessible eye toggle.

### 3. Executive Recovery Consoles & Data Visualizations
- **Recovery Cases Executive Console (`RecoveryCaseDetailPage.tsx`)**: High-trust layout featuring a 6-card intelligence matrix: Payment Summary, AI Diagnosis & Reasoning, Recovery Strategy, Timeline Attempts, Simulation Console, and Cancellation Flow.
- **Accessible Multi-Series SVG Chart (`RecoveryTrendChart.tsx`)**: Gradient area curves, custom tooltips, dual metric toggles, and full screen-reader-accessible fallback data tables.
- **Event-Channel Notification Matrix (`NotificationPreferencesMatrix.tsx`)**: Interactive 4x3 preference grid with instant toggle feedback, dirty state tracking, and webhook endpoint validation.


## Setup & Commands

### Prerequisites
- Node.js 20+ (Node 22 LTS recommended)
- npm 10+

### Environment Configuration
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
Default variable:
```env
VITE_API_BASE_URL=http://localhost:8080
```

### Install Dependencies
```bash
npm install
```

### Start Development Server
```bash
npm run dev
```
Development server runs at `http://localhost:5173`.

### Run Automated Tests
```bash
npm test -- --run
```
Executes all 135 Vitest unit, component, routing, analytics, recovery case, notification, preferences, provider status, and interactive demo mode workflow test suites in headless JSDOM mode across 20 test files.

### Production Build
```bash
npm run build
```
Executes TypeScript type check (`tsc -b`) followed by Vite production bundling.

### Linting
```bash
npm run lint
```
Runs Oxlint across all TypeScript source files with 0 errors and 0 warnings.
