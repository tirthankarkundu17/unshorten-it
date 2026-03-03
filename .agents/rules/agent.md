---
trigger: always_on
---

# AGENT.md

You are working inside a mono repo containing:

- /backend → FastAPI (Python)
- /frontend → React + Vite (TypeScript)
- /android → Kotlin Android app
- /shared (optional) → contracts / generated code

Your goal is to implement features as complete vertical slices across backend, frontend, and Android.

--------------------------------------------------
CORE PRINCIPLES
--------------------------------------------------

1. Backend is the source of truth.
2. API contracts originate from FastAPI Pydantic models.
3. Frontend and Android must strictly follow backend schemas.
4. Never invent API fields.
5. Never change API contracts silently.
6. Prefer explicit types everywhere.
7. Never use `any` in TypeScript.
8. Never block main thread in Android.
9. Business logic belongs in backend services, not UI layers.

--------------------------------------------------
FEATURE DEVELOPMENT PROTOCOL
--------------------------------------------------

When implementing a feature, follow this order strictly:

STEP 1 — Define backend schema
STEP 2 — Implement backend service logic
STEP 3 — Implement backend route with response_model
STEP 4 — Show example JSON response
STEP 5 — Generate matching TypeScript types
STEP 6 — Generate matching Kotlin data classes
STEP 7 — Implement frontend API call
STEP 8 — Implement frontend hook/state
STEP 9 — Implement frontend UI
STEP 10 — Implement Android Retrofit API
STEP 11 — Implement Android Repository
STEP 12 — Implement Android ViewModel
STEP 13 — Implement Android UI state handling

Never reverse this order.

A feature is not complete unless:
- Backend works
- Web works
- Android works
- Loading states handled
- Error states handled

--------------------------------------------------
BACKEND RULES (FastAPI)
--------------------------------------------------

Structure:

backend/                 # FastAPI application
│   ├── app/                # Application routes and services
│   ├── pyproject.toml      # Dependency definitions
│   └── Dockerfile

Rules:

- Always use Pydantic models.
- Always use response_model in routes.
- Always use async def for endpoints.
- Never return raw dicts.
- Never put business logic inside routes.
- Validation happens only in Pydantic.
- Use dependency injection for DB/session.
- All routes must be versioned: /api/v1/...

Standard Error Format:

{
  "error": {
    "code": "STRING_CODE",
    "message": "Human readable message",
    "details": {}
  }
}

--------------------------------------------------
FRONTEND RULES (React + Vite + TypeScript)
--------------------------------------------------

Structure:

frontend/src/
    api/
    components/
    hooks/
    pages/
    types/

Rules:

- Always use TypeScript.
- Never use `any`.
- Never duplicate backend validation logic.
- API calls must go through a central API client.
- Server state → React Query (or equivalent).
- UI state → useState / useReducer.
- Pages fetch data.
- Components are presentational.
- No API calls inside deeply nested components.

Snake_case from backend must be converted safely if needed.
Types must exactly mirror backend models.

--------------------------------------------------
ANDROID RULES (Kotlin)
--------------------------------------------------

Structure:

android/
  data/
    api/
    repository/
  domain/
  ui/
    screens/
    viewmodel/

Rules:

- Use Retrofit.
- Use Coroutines (suspend functions).
- Use ViewModel + StateFlow.
- Never block main thread.
- Never use callbacks when coroutines are possible.
- Never hardcode base URLs.
- API models must mirror backend schemas.
- Use sealed classes for UI state:

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

--------------------------------------------------
CONTRACT SYNCHRONIZATION
--------------------------------------------------

When backend schema changes:

You MUST:
1. Update frontend types.
2. Update Android data classes.
3. Update API interfaces.
4. Update affected UI.

Never leave clients inconsistent with backend.

If OpenAPI generation is available:
- Prefer generating TypeScript and Kotlin models.
- Avoid manually duplicating API contracts.

--------------------------------------------------
ANTI-PATTERNS (STRICTLY FORBIDDEN)
--------------------------------------------------

- ❌ Business logic in UI
- ❌ Silent API contract changes
- ❌ Hardcoded mock data in production
- ❌ Skipping loading states
- ❌ Skipping error handling
- ❌ Massive 500+ line components
- ❌ Duplicating validation rules across platforms
- ❌ Returning inconsistent error shapes
- ❌ Using `any` in TypeScript
- ❌ Blocking Android main thread

--------------------------------------------------
TESTING EXPECTATIONS
--------------------------------------------------

Backend:
- Use pytest
- Test services, not just routes

Frontend:
- Test hooks and critical UI states

Android:
- Test ViewModels
- Mock repositories

--------------------------------------------------
DECISION PRIORITY
--------------------------------------------------

When uncertain, prioritize:

1. Correctness
2. Type safety
3. Separation of concerns
4. Consistency with existing code
5. Performance
6. Developer experience

--------------------------------------------------
OUTPUT EXPECTATION FROM AGENT
--------------------------------------------------

When implementing something:

- Generate complete working slices.
- Do not provide partial fragments unless explicitly requested.
- If context is missing, scaffold full files instead of patching unknown code.
- Be explicit about assumptions.
- Keep code clean, readable, and production-ready.

--------------------------------------------------
END OF RULES
--------------------------------------------------