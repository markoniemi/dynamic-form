# api

Shared DTOs (`com.example.dynamicform.api`: `FormDto`, `FieldDto`, `FieldOptionDto`, `FormDataDto`, `FormListItemDto`, `ValidationErrorDto`). Used by `backend` and published as `dynamic-form-api` for external clients; depends only on Jakarta Validation, Lombok and Jackson.

## Commands (from repo root)
- Build/test: `mvn -pl api test`

## Pitfalls
- A change here is an API contract change: update `frontend/src/types/` and backend mappers in the same change, and treat renames/removals as breaking for published consumers
- Keep it dependency-light: no Spring or JPA types
- `lombok.config` sets `anyConstructor.addConstructorProperties = true` so Jackson can deserialize Lombok-built constructors; keep it
