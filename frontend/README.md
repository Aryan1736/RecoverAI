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
│   ├── client.ts             # Centralized HTTP client (fetch, interceptors, 401 handler, base URL)
│   ├── auth.ts               # Login, registration, and backend health check API calls
│   └── dashboard.ts          # Dashboard overview metrics API call
├── components/
│   ├── layout/
│   │   ├── AppShell.tsx      # Master dashboard shell with responsive sidebar & top navigation
│   │   ├── Sidebar.tsx       # Collapsible navigation drawer with live engine status
│   │   └── Header.tsx        # Top navigation with live backend health & merchant profile dropdown
│   └── ui/
│       ├── Alert.tsx         # Semantic alert banners (info, success, warning, error)
│       ├── Avatar.tsx        # Dynamic initials avatar with status indicator
│       ├── Badge.tsx         # Status pills with optional live pulse dots
│       ├── Button.tsx        # Accessible button variants with built-in loading spinner
│       ├── Card.tsx          # Composable card primitives (Header, Title, Content, Footer)
│       ├── EmptyState.tsx    # Production empty state with icons and action buttons
│       ├── ErrorState.tsx    # User-friendly error recovery container with retry action
│       ├── Input.tsx         # Form control with labels, helper text, and accessible error states
│       ├── PageHeader.tsx    # Standardized page title, description, and action bar
│       ├── PasswordInput.tsx # Secure password input with show/hide eye toggle
│       └── Skeleton.tsx      # Shimmer loading placeholders for cards and metrics
├── context/
│   ├── auth-context-def.ts   # Core AuthContext definition
│   ├── AuthContext.tsx       # Authentication session provider & state lifecycle
│   ├── toast-context-def.ts  # Core ToastContext definition
│   └── ToastContext.tsx      # Non-blocking floating toast notifications provider
├── hooks/
│   ├── useAuth.ts            # Hook to access current merchant, token, and session actions
│   └── useToast.ts           # Hook to trigger accessible notifications (success, error, info)
├── pages/
│   ├── auth/
│   │   ├── LoginPage.tsx     # Merchant authentication screen with client validation
│   │   └── RegisterPage.tsx  # Merchant onboarding form matching backend DTO
│   ├── dashboard/
│   │   └── OverviewPage.tsx  # Landing overview with real metrics, pipeline status, and webhook guide
│   └── NotFoundPage.tsx      # 404 fallback page with navigation recovery
├── routes/
│   └── AppRoutes.tsx         # Centralized routing with ProtectedRoute and PublicRoute guards
├── types/
│   ├── api.ts                # ApiErrorResponse, ApiError class, and error sanitization helper
│   ├── auth.ts               # Merchant, AuthResponse, LoginRequest, RegisterRequest DTOs
│   └── dashboard.ts          # DashboardSummary response interface
├── test/
│   ├── setup.ts              # Vitest test setup and DOM polyfills
│   ├── apiClient.test.ts     # Bearer header attachment, 401 expiry, and error parser tests
│   ├── auth.test.tsx         # Login, register, validation, and session expiry UI tests
│   ├── routing.test.tsx      # Route guards, authenticated redirects, and 404 tests
│   ├── components.test.tsx   # Reusable UI component unit tests
│   └── accessibility.test.tsx# Semantic labels, ARIA attributes, and keyboard tab navigation tests
├── App.tsx                   # Root component wrapping Router, ToastProvider, and AuthProvider
├── index.css                 # Tailwind CSS v4 stylesheet and theme tokens
└── main.tsx                  # Application entry point
```

---

## Route Architecture

| Route | Guard | Description |
| :--- | :--- | :--- |
| `/login` | `PublicRoute` | Merchant login screen. Redirects to `/app` if already authenticated. |
| `/register` | `PublicRoute` | Merchant registration screen. Redirects to `/app` if already authenticated. |
| `/app` | `ProtectedRoute` | Authenticated merchant portal dashboard. Redirects to `/login` if unauthenticated. |
| `/` | Redirect | Automatically redirects to `/app` (which routes to `/login` if session is missing). |
| `*` | Catch-all | Accessible 404 Not Found error page with recovery link. |

---

## Authentication & Security Model

1. **Authoritative Backend**: The frontend never trusts client-side state for authorization. The backend Spring Boot security filters remain authoritative.
2. **JWT Storage**: The access token is retained in `localStorage` under `recoverai_token` with the merchant profile in `recoverai_merchant`.
3. **Session Invalidation (401 Interception)**: Any authenticated API call that encounters a `401 Unauthorized` triggers an unauthenticated event in `apiClient`. This automatically clears stored credentials and redirects to `/login` with a safe "Your session has expired" message, preventing infinite redirect loops.
4. **Error Sanitization**: Raw backend stack traces or exceptions (`NullPointerException`, SQL errors) are never exposed. All errors pass through `getHumanReadableErrorMessage()` to provide safe, actionable guidance.
5. **No Secret Leaks**: Webhook secrets, database credentials, and JWT signing keys are never exposed or placed in `VITE_*` public variables.

---

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
npm run test
```
Executes all 39 Vitest unit, component, routing, and accessibility test suites in headless JSDOM mode.

### Production Build
```bash
npm run build
```
Executes TypeScript type check (`tsc -b`) followed by Vite production bundling.

### Linting
```bash
npm run lint
```
Runs Oxlint across all TypeScript source files.
