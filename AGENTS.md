# Repository Guidelines

## Project Structure & Modules
- Root: planning docs (see `requirements.md`).
- Implement two apps with identical features:
  - `spring-app/`: Spring Boot service + web UI.
  - `javalin-app/`: Javalin service + web UI.
- Common layout per app: `src/main/java`, `src/main/resources`, `src/test/java`, `docs/`, `README.md`.
- Shared domain model: `LogEntry` (id, timestamp, level, source, message, username, tags, status).

## Build, Test, Run
- Maven (Spring): `./mvnw clean verify` — build + unit tests.
- Run (Spring): `./mvnw spring-boot:run` — start local server.
- Gradle (Javalin): `./gradlew clean test` — build + unit tests.
- Run (Javalin): `./gradlew run` — start local server.
- Integration tests (if present): `-Pit` profile or `*IT` tests via `failsafe`/separate Gradle task.

## Coding Style & Naming
- Java 17, 4-space indent, 120-col soft wrap.
- Packages: `uni.logbook.<module>`; classes `PascalCase`, methods/fields `camelCase`.
- Tests mirror package of SUT; name `ClassNameTest` (unit) and `ClassNameIT` (integration).
- Prefer constructor injection, final fields, and immutability where reasonable.
- Formatting/linting: use Spotless + Google Java Format and Checkstyle if configured; run `./mvnw spotless:apply` or `./gradlew spotlessApply`.

## Testing Guidelines
- Frameworks: JUnit 5, AssertJ; Mockito for stubs.
- Coverage target: ≥80% for domain and service layers; critical paths in controllers covered by Web tests.
- Include repository tests with an in-memory DB (H2) and slice tests for controllers/services.
- Test data builders and parameterized tests for filters (date range, level, source, text).

## Commit & PR Guidelines
- Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `build:`, `chore:`.
- Keep changes parallel across both apps; note symmetry in PR description.
- PRs include: summary, linked issue, screenshots or curl examples, setup notes, and risks.
- Small, focused PRs; add migration notes for DB schema changes.

## Security & Config
- Do not commit secrets. Use env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD`.
- Provide `.env.example` per app and default to H2 for local runs.
- Validate inputs and log safely (no PII). Use structured logging for filters and queries.
