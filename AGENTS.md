# Repository Guidelines

## Project Structure & Module Organization
- Typical Spring Boot layout: `src/main/java` (application code), `src/main/resources` (config, `application.yml`), `src/test/java` (unit/integration tests).
- Static assets and templates: `src/main/resources/static/` and `src/main/resources/templates/`.
- If multiple modules are used, each module is a subfolder with its own `build.gradle`/`pom.xml`.

## Build, Test, and Development Commands
- Maven: `mvn spring-boot:run` (run app), `mvn test` (unit tests), `mvn verify` (tests + checks), `mvn clean package` (build JAR/WAR).
- Wrapper: not committed; use system Maven 3.9+ and Java 17.
- Profiles: default uses in-memory H2; activate PostgreSQL via `-Dspring.profiles.active=postgres`.

## Coding Style & Naming Conventions
- Java style: 4-space indent, UTF-8, max line length ~120 where practical.
- Naming: `PascalCase` classes, `camelCase` methods/fields, `UPPER_SNAKE_CASE` constants, package names all-lowercase (e.g., `com.example.logbook`).
- Formatting/linting: if Checkstyle/Spotless is configured, run via the build (`verify`/`check`). Keep imports ordered and avoid unused code.

## Testing Guidelines
- Frameworks: JUnit 5 and Spring Boot Test are expected for unit/integration tests.
- Location: mirror package structure under `src/test/java`.
- Naming: `ClassNameTests` for unit tests; `*IT` for integration tests if used.
- Run: `./mvnw test` or `./gradlew test`. Aim for meaningful coverage on services, controllers, and repositories.

## Commit & Pull Request Guidelines
- Commits: present tense, concise subject (<72 chars), detailed body when needed. Reference issues (`Fixes #123`). Group related changes.
- Branching: short, kebab-case names like `feature/add-entry-api` or `fix/validation-bug`.
- PRs: include summary, linked issues, steps to test; attach screenshots for UI-affecting changes. Ensure all checks pass and code is formatted.

## Security & Configuration Tips
- Do not commit secrets. Use environment variables or profile-specific configs (`application-dev.yml`, `application-prod.yml`).
- Use `.env` for local-only secrets; copy `.env.example` to `.env` and adjust. The app loads `.env` via `spring-dotenv`.
- Use `SPRING_PROFILES_ACTIVE=dev` (or `postgres`) for local runs; keep production credentials out of the repo.
- `.gitignore` excludes `.env` and `.env.*` (except `.env.example`).

## Notable Changes in This Commit
- Added a Spring Boot Maven project under `src/main/java` and `src/main/resources` with Java 17.
- Implemented `LogEntry` entity with ≥7 attributes and indexes for filter fields.
- Added REST API `GET/POST/PUT/DELETE /api/logs` with paging and filters; simple CSV/JSON export at `/api/logs/export`.
- Configured default H2 for dev/testing and a `postgres` profile for PostgreSQL via env vars (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`).
- Introduced JPA Specifications for dynamic filtering and a minimal JPA test.
- No Maven Wrapper included to avoid committing binaries; use system `mvn`.
- Added `@ControllerAdvice` (`RestExceptionHandler`) to standardize validation errors, 404s, and generic errors.
- Added Web MVC tests for the controller under `src/test/java/com/example/logbook/web/`.
- Added `scripts/curl-examples.sh` and `postman/Logbook.postman_collection.json` for quick API testing.
- Introduced `Server` entity and repository; `LogEntry` now references a `Server` (FK, indexed).
- New endpoints under `/api/servers` to list/create/fetch servers and to list logs per server.
- File upload endpoint `POST /api/servers/{id}/logs/upload` now supports single (`file`) and multi-file (`files`) uploads; response includes per-file counts and total. Parser recognizes ISO-8601, log4j-style, and syslog-style lines.
- Added `.env` support with `spring-dotenv`; see `.env.example`. Postgres connection params are read from env.
