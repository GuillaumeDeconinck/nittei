# Java Translation Scaffold (Java 21)

This folder contains a Java 21 multi-module Maven scaffold that mirrors the Rust API setup from:

- `bins/nittei`
- `crates/api`

## Modules

- `nittei-utils`: shared utility module (config model, random secret helper, filtered backtrace helper)
- `nittei-infra`: infrastructure module (DB pool + migrations + infra context + repository contracts + OAuth provider adapters)
- `nittei-app`: runtime/bootstrap module (application entrypoint, shutdown behavior, startup hooks)
- `nittei-api`: web/API module (controllers, auth guards, middleware/filter, OpenAPI config, scheduler stubs)

## Scope of this first step

- Route surface is replicated under `/api/v1`.
- Startup wiring and common infrastructure are in place.
- Endpoint business logic, domain structures, and DTOs are intentionally not implemented yet.
- Most endpoint handlers currently return `501 Not Implemented`.
- Infra repository contracts are defined and wired; only `status.checkConnection` is concrete so far, with the rest of Postgres adapters scaffolded for incremental implementation.

## Run

```bash
mvn -f java/pom.xml spring-boot:run -pl nittei-app -am
```

Swagger UI:

- `/swagger-ui`

OpenAPI JSON:

- `/api-docs/openapi.json`
