# ClaimWise AI

## Project Overview
ClaimWise AI is an enterprise expense claim policy review assistant. It streamlines the employee expense reimbursement process by combining deterministic backend validation with advanced external Large Language Model (LLM) analysis. The system provides immediate, grounded feedback to claimants and generates advisory analysis to assist human reviewers in making compliant, efficient decisions.

## Problem Statement
(Problem 1) Organizations struggle with the manual review of expense claims. Reviewers must cross-reference complex, category-specific policies, verify receipt requirements, and detect anomalies or duplicate claims. This manual process is error-prone, time-consuming, and inconsistent.

## Solution Summary
ClaimWise AI solves this by introducing a dual-engine review system:
1. **Deterministic Validation Engine**: Handles strict rules (amount limits, dates, duplicate detection, missing receipts).
2. **AI Policy Review Engine**: Evaluates claim descriptions against grounded, database-stored organizational policies to determine business rationale and compliance.

## Key Features
* **Role-Based Access Control**: Separate views and permissions for Employees and Reviewers.
* **Claim Submission & Dashboard**: Employees can submit claims (Travel, Meals, Accommodation, etc.) and track their status via a comprehensive dashboard.
* **Deterministic Rule Validation**: Hard limits on maximum amounts, missing receipt checks, date bounds, duplicate claim detection, and currency verification.
* **AI-Powered Policy Analysis**: Integration with an external LLM (e.g., OpenAI) to analyze claims against specific DB-stored policy rules. 
* **Database-Grounded Evidence**: AI is strictly fed policy rules from the database to prevent hallucinations.
* **Structured Output Validation**: The system strictly parses and validates the JSON output from the LLM.
* **Fallback Engine**: A resilient secondary structured analysis engine takes over if the external LLM API is unavailable, unconfigured, or fails.
* **Human Reviewer Workflow**: Reviewers see the deterministic issues and the AI's advisory analysis (including confidence score and missing information) before making a final Approve, Reject, or Request Clarification decision.
* **Audit Trail**: Every claim action (creation, AI review, human decision) is securely logged.

## Technology Stack
* **Frontend**: React (Vite), JavaScript, Vanilla CSS, React Router DOM, Recharts (for Dashboard).
* **Backend**: Java 21, Spring Boot 3, Spring Security (JWT).
* **Database**: H2 (Development) / MySQL (Production via JPA/Hibernate).
* **AI Integration**: Custom HTTP Client integration with OpenAI-compatible endpoints.
* **Build Tools**: Maven (Backend), npm (Frontend).

## System Architecture
* **Frontend Client**: SPA handling user interactions, JWT storage in local storage, and rendering dashboards.
* **Spring Boot REST API**: Stateless backend exposing secured endpoints.
* **Service Layer**: Orchestrates the validation engine, AI integration, and core business logic.
* **Database Layer**: JPA Repositories interacting with the relational database.

## Folder Structure
```
ClaimWise AI/
├── backend/
│   ├── src/main/java/com/claimwise/
│   │   ├── client/       # External LLM HTTP Client
│   │   ├── config/       # Security & CORS configuration
│   │   ├── controller/   # REST API Endpoints
│   │   ├── dto/          # Data Transfer Objects
│   │   ├── entity/       # JPA Entities
│   │   ├── exception/    # Global Exception Handler
│   │   ├── repository/   # Spring Data JPA Repositories
│   │   ├── security/     # JWT Auth Filters & Services
│   │   └── service/      # Business Logic, AI, Validation, Audit
│   └── src/test/         # JUnit & Spring Boot Tests
└── frontend/
    ├── src/
    │   ├── assets/       # Static assets
    │   ├── components/   # Reusable UI components
    │   ├── context/      # React Context (Auth)
    │   ├── pages/        # Route pages (Dashboard, Queue, etc.)
    │   ├── services/     # API Axios client
    │   └── App.jsx       # Main App Component
    └── package.json
```

## Database Entities
* `User`: Stores employee and reviewer credentials and roles.
* `ExpenseClaim`: Core entity storing claim details (amount, category, date, description).
* `Policy`: Stores organizational rules per category (e.g., max amounts, receipt rules).
* `AiReview`: Stores the parsed output from the LLM or Fallback Engine.
* `ReviewDecision`: Stores the final human decision and reasoning.
* `AuditTrail`: Immutable ledger of all system events tied to a claim.

## Authentication and Role-Based Access
* Uses JWT (JSON Web Tokens) for stateless authentication.
* Passwords are encrypted using BCrypt.
* `Role.EMPLOYEE`: Can submit claims and view their own claims.
* `Role.REVIEWER`: Can view the reviewer queue and approve/reject any claim.

## Deterministic Validation Workflow
Before AI analysis, the `DeterministicValidationService` checks:
1. Valid amounts and descriptions.
2. Expense dates (no future dates).
3. Maximum amounts and receipt requirements queried directly from the `Policy` table.
4. **Duplicate Claim Detection**: Checks for previously submitted claims by the same user with the same date, amount, or category.
5. Currency validation.

## AI Policy Review Workflow
1. The backend gathers claim details, deterministic validation results, and relevant active policies from the database.
2. It constructs a strict prompt requesting a JSON object adhering to a specific schema.
3. The `ExternalLlmClient` makes a network call to the configured provider (e.g., OpenAI).
4. The response is parsed, and citations are verified against the database (hallucinated policy codes are stripped).
5. The result is saved to the `AiReview` table.

## Structured AI Response Validation
The AI must return a JSON object with:
* `classification` (COMPLIANT, NEEDS_REVIEW, etc.)
* `confidence` (0.0 to 1.0)
* `uncertain` (boolean)
* `summary`, `reasoning`, `missingInformation`, `policyEvidence`, `recommendedAction`.
The backend validates these fields and enforces enum constraints.

## Fallback Engine Behavior
If the LLM API times out, fails, or is missing an API key, the system transparently falls back to a deterministic rule-based engine that generates a structured `AiReviewAnalysis`. This ensures the review workflow is never blocked by external provider outages.

## Human Reviewer Approval Workflow
1. Reviewer opens the Reviewer Queue.
2. Selects a claim and views the detailed breakdown.
3. Reviews the Deterministic Validation warnings and the AI Analysis advisory.
4. Submits a final decision (Approve, Reject, Clarification) with a required reason.
5. Action is recorded in the `ReviewDecision` and `AuditTrail` tables.

## API Endpoints
* `POST /api/v1/auth/login` - Authenticate and get JWT.
* `POST /api/v1/claims` - Submit a new claim.
* `GET /api/v1/claims/my-claims` - Get claims for logged-in user.
* `GET /api/v1/claims/reviewer/queue` - Get pending claims (Reviewer only).
* `GET /api/v1/claims/{id}` - Get full claim details.
* `GET /api/v1/claims/{id}/validation` - Get deterministic issues.
* `POST /api/v1/claims/{id}/ai-review` - Trigger AI review manually.
* `GET /api/v1/claims/{id}/ai-review` - Get stored AI review.
* `POST /api/v1/claims/{id}/review/approve` - Approve claim (Reviewer only).
* `POST /api/v1/claims/{id}/review/reject` - Reject claim (Reviewer only).
* `GET /api/v1/claims/{id}/audit-trail` - Get audit logs.
* `GET /api/v1/dashboard/stats` - Get dashboard statistics.
* `GET /api/v1/policies` - Get all policies.

## Local Setup Instructions

### Prerequisites
* Java 21
* Maven
* Node.js v18+
* (Optional) MySQL (App defaults to in-memory H2 for development)

### Required Environment Variables
Create a `.env` file in the `backend` or export these variables:
```
# See .env.example for required variables
JWT_SECRET=your_secure_random_string
AI_API_KEY=your_openai_api_key
```

### Backend Setup
1. Navigate to `backend/`.
2. Run: `mvn clean install`
3. Run: `mvn spring-boot:run`
Backend runs on `http://localhost:8080`.

### Frontend Setup
1. Navigate to `frontend/`.
2. Run: `npm install`
3. Run: `npm run dev`
Frontend runs on `http://localhost:5173`.

### Test Commands and Verified Results
* Run `mvn clean test` in the `backend/` directory.
* Result: 100% Pass (All 15 tests, including Claim Creation, Deterministic Validation, AI JSON parsing, Fallback behavior, Duplicate Detection, and Role-Based Security, pass successfully).

## Demo Accounts
The system automatically seeds the following development accounts:
* **Employee**: `employee@claimwise.com` / `Employee123!`
* **Reviewer**: `reviewer@claimwise.com` / `Reviewer123!`
* **Admin**: `admin@claimwise.com` / `Admin123!`

## Limitations and Excluded Scope
* No actual receipt image OCR processing (receipt requirement is a boolean flag for the assessment).
* Email notifications are mocked/excluded.
* Multi-currency conversion rates are hardcoded/excluded (the app enforces a single standard currency or flags anomalies).
* Advanced real-time websockets for queue updates are excluded.

## Security Considerations
* JWT tokens are used for authentication.
* Passwords are not stored in plaintext.
* Endpoints are protected by Spring Security `@PreAuthorize` based on Roles.
* A hardcoded JWT secret was found in the initial commit and has been removed from `application.yml`. (Recommended action: Any deployed instance using the old hardcoded secret should rotate it immediately via environment variables).

## Future Improvements
* Integrate OCR for automated receipt data extraction.
* Implement a robust FX rate API for multi-currency claims.
* Add comprehensive frontend end-to-end tests (Cypress/Playwright).
