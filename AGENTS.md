# Repository Guidelines

## Project Structure & Module Organization
- Typical Spring Boot layout: `src/main/java` (application code), `src/main/resources` (config, `application.yml`), `src/test/java` (unit/integration tests).
- Static assets and templates: `src/main/resources/static/` and `src/main/resources/templates/`.
- If multiple modules are used, each module is a subfolder with its own `build.gradle`/`pom.xml`.

## Build, Test, and Development Commands
- Maven: `./mvnw spring-boot:run` (run app), `./mvnw test` (unit tests), `./mvnw verify` (tests + checks), `./mvnw clean package` (build JAR/WAR).
- Gradle: `./gradlew bootRun` (run app), `./gradlew test` (unit tests), `./gradlew check` (tests + checks), `./gradlew clean build` (build artifact).
- Use the wrapper (`mvnw`/`gradlew`) if present; otherwise use `mvn`/`gradle`.

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
- Use `SPRING_PROFILES_ACTIVE=dev` for local runs; keep production credentials out of the repo.
- `.gitignore` already excludes common Java artifacts; add entries for any generated files specific to your setup.

