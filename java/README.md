# Java Translation Scaffold (Java 25)

This folder contains a Java 25 multi-module Maven scaffold that mirrors the Rust API setup from:

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
./mvnw -f java/pom.xml spring-boot:run -pl nittei-app -am
```

Or use the helper script:

```bash
./scripts/dev_java.sh
```

## Docker

The repository now includes a production-oriented Dockerfile for the Java runtime at `java/Dockerfile`.

Build the image from the repository root:

```bash
docker build -f java/Dockerfile -t nittei-java:local .
```

Run it locally:

```bash
docker run --rm -p 8080:8080 \
  -e NITTEI_PG_DATABASE_URL='jdbc:postgresql://host.docker.internal:45432/nittei?user=postgres&password=postgres' \
  -e NITTEI_PG_SKIP_MIGRATIONS=false \
  nittei-java:local
```

Container notes:

- The image is multi-stage and builds the runnable `nittei-app` artifact inside Docker.
- The runtime image uses Spring Boot layer extraction, which keeps rebuilds and registry pushes more efficient when only app code changes.
- The runtime container runs as a non-root user.
- The container starts with a shell-free entrypoint and uses `JAVA_TOOL_OPTIONS` for JVM tuning, which is friendlier to k8s and later distroless migration.
- The default JVM flags are container-aware and suitable for k8s memory limits.
- The app listens on port `8080` by default and binds to `0.0.0.0`.
- Request health endpoints are exposed through Spring Boot Actuator under `/actuator/health` and the existing API healthcheck under `/api/v1/healthcheck`.

## Quality Gates

The Java build now runs a small baseline quality gate set during `mvn verify`:

- Java and Maven version enforcement
- Spotless formatting checks for Java and POM files
- JaCoCo coverage reports and minimum line coverage checks for the currently tested modules
- SpotBugs static analysis checks for the currently gated modules

Common local commands:

```bash
./scripts/quality_java.sh
./scripts/quality_java.sh --fix
./mvnw -f java/pom.xml verify
```

You can also forward normal Maven reactor flags through the helper when you want a tighter loop:

```bash
./scripts/quality_java.sh -pl nittei-api -am
./scripts/quality_java.sh --fix -pl nittei-infra -am
```

By default the helper uses `MAVEN_LOCAL_REPO=/tmp/nittei-m2` so repeated local runs stay fast and do not interfere with your main Maven cache.

The repository now includes a Maven wrapper pinned to Maven `3.8.7`, and the Java helper scripts prefer that wrapper automatically. You can still override the Maven binary explicitly with `MAVEN_BIN=/path/to/mvn` if needed.

The same quality command is also run in GitHub Actions via `.github/workflows/java-quality.yml`.

Coverage reports are generated under each module's `target/site/jacoco/` directory when `verify` runs.

SpotBugs reports are generated under each module's `target/spotbugsXml.xml` output during the gated modules' verify run.

Swagger UI:

- `/swagger-ui`

OpenAPI JSON:

- `/api-docs/openapi.json`
