# Kreya Backend

Minimal Spring Boot foundation for the Kreya backend.

## What is included

- Java 25 + Spring Boot 4 bootstrap
- Spring Modulith dependency and package layout foundation
- Ping endpoint under `/api/v1/ping`
- Minimal shared package structure
- Docker Compose for local dependencies
- Flyway baseline schema migration
- OpenAPI and Actuator exposure
- Basic startup and endpoint tests

## Repository layout

- `docs/specs` holds the project specifications
- `src/main/java/com/kreya` holds the application code
- `src/main/resources/db/migration` holds Flyway migrations
- `src/test/java/com/kreya` holds the tests

## Run locally

1. Start infrastructure with Docker Compose.
2. Run the application with the `dev` profile.
3. Open Swagger UI at `/swagger-ui/index.html`.
4. Check the ping endpoint at `/api/v1/ping`.

The default PostgreSQL URL pins the database session timezone to `UTC` so local machine timezone aliases do not affect startup.

## Current scope

This bootstrap only establishes the platform foundation. Business modules and persistence-backed features come in the next implementation pass.
