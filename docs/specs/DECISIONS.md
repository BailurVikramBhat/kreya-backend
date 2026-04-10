# Backend Architecture Decisions

## 2026-04-09
- Adopted a backend-first workflow. UI tickets are tracked separately and are intentionally excluded from these backend spec notes.
- Chose a modulith package layout with module ownership split across `auth`, `user`, `seller`, `product`, `admin`, and shared infrastructure packages.
- Kept persistence schema changes under Flyway migrations rather than Hibernate auto-DDL.
- Split identity data between `user_schema.users` and `auth_schema.credentials`, with credential data owned by the `auth` module and profile/role identity owned by the `user` module.
- Used one shared login flow for all users instead of separate role-specific login pages or endpoints.
- Standardized API responses on the shared `ApiResponse` envelope.
- Chose JWT-based authentication with a shared `JwtProvider`, access token and refresh token issuance in the auth service, and configuration-driven secret/expiry values.
- Added CI branch filtering for `master`, release branches, and feature branches, with test report artifact upload only on `master`.
- Chose a maintainable frontend direction for later implementation: single website login, Mantine component library, and no vintage-themed design work.

## 2026-04-10
- Added in-repo backend project memory files under `docs/spec` to track completed work, completed ticket ids, and architecture decisions over time.
