# Repository Guidelines

This repository hosts a Javalin web application. Use the guidelines below to keep contributions consistent, reviewable, and easy to run locally.

## Project Structure & Module Organization

Recommended layout for this branch:

```
src/main/java|kotlin/...        # application sources (packages in reverse‑DNS)
src/main/resources/public/      # static assets (CSS/JS/images)
src/main/resources/             # config, templates (e.g., Thymeleaf/Freemarker)
src/test/java|kotlin/...        # unit/integration tests
build.gradle(.kts) or pom.xml   # build definition
README.md, AGENTS.md            # docs
```

Keep classes small and grouped by feature (routes, services, repositories). Place HTTP handlers under `.../web` or `.../controller` and domain logic under `.../core`.

## Build, Test, and Development Commands

- Gradle: `./gradlew clean build` (compile + run tests), `./gradlew test` (tests only), `./gradlew run` (if `application` plugin configured), `java -jar build/libs/<app>.jar` (run packaged jar).
- Maven: `mvn clean package` (compile + tests), `mvn test` (tests only), `mvn exec:java -Dexec.mainClass=com.example.App` (run with exec plugin).

If both build files exist, prefer Gradle. Use a Java 17+ runtime unless documented otherwise.

## Coding Style & Naming Conventions

- Indentation: 4 spaces; UTF‑8; LF line endings.
- Java/Kotlin: UpperCamelCase for types, lowerCamelCase for members, CONSTANT_CASE for constants. Package names are lowercase.
- Keep controllers thin; move business logic to services. Avoid static state in request handlers.
- Formatting: prefer Google Java Format or `ktlint`/`ktfmt`. Run the project formatter before committing when available.

## Testing Guidelines

- Framework: JUnit 5 (Java) or Kotest/JUnit 5 (Kotlin). Aim for ≥80% line coverage on core modules.
- Naming: test classes end with `*Test` (or `*IT` for integration). Use `given_when_then` in test method names.
- Commands: `./gradlew test` or `mvn test`. Generate coverage with `./gradlew jacocoTestReport` or the JaCoCo Maven plugin.

## Commit & Pull Request Guidelines

- Commits: follow Conventional Commits (e.g., `feat: add entry export API`, `fix: handle null user id`). Keep changes scoped and atomic.
- PRs: include a clear description, linked issue, test coverage for new logic, and screenshots for UI changes. Note any breaking changes and migration steps.
- Checks: ensure build, tests, and linters pass locally before requesting review.

## Security & Configuration Tips

- Do not commit secrets. Use environment variables and provide `src/main/resources/application.properties` defaults. Add examples in `.env.example`.
- Validate all inputs at the edge; never trust request bodies or query params. Log at INFO/DEBUG; avoid logging PII.

