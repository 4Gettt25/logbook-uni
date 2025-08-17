Spring Logbook (Maven, Spring Boot)

Overview
- Minimal log management app with a small UI (Thymeleaf + Bootstrap) and REST APIs.
- Entities: `Server` and `LogEntry` (logs reference a server via FK).
- Features: CRUD, paging + filtering, CSV/JSON export, file upload with multi-format parsing, Flyway migrations.

Project layout
- App code: `src/main/java`
- Resources: `src/main/resources` (`application.yml`, Flyway migrations, static assets, templates)
- Tests: `src/test/java` and `src/test/resources`

Run
- Prereqs: Java 17, Maven 3.9+.
- Dev (H2 persistent): `mvn spring-boot:run`
- Tests: `mvn test`
- Package: `mvn clean package`

Database configuration
- Default profile uses a file-based H2 database so data persists across restarts.
  - Files under `./data/logbook.*` (ignored by Git).
  - H2 console: `/h2-console` (Driver: `org.h2.Driver`, JDBC URL: `jdbc:h2:file:./data/logbook`).
- PostgreSQL profile (`postgres`):
  1) Copy `.env.example` to `.env` and set:
     - `SPRING_PROFILES_ACTIVE=postgres`
     - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
  2) `mvn spring-boot:run` (or set `-Dspring-boot.run.profiles=postgres`).
  3) Flyway validates/migrates; JPA `ddl-auto: update` remains enabled.

Environment (.env)
- `.env` is imported early via `spring.config.import` so profile and DB settings are available at bootstrap.
- You can also set OS environment variables instead of `.env`.
- Tests set an empty `spring.config.import` and use H2, so they do not depend on `.env`.

REST API
- `GET /api/logs` filters: `from`/`to` (ISO-8601), `level` (repeatable), `source`, `q` (message contains), `page`, `size`.
- `GET /api/logs/{id}` fetch one.
- `POST /api/logs` create.
- `PUT /api/logs/{id}` update.
- `DELETE /api/logs/{id}` delete.
- `GET /api/logs/export?format=csv|json&limit=N` export first N items matching filters.

Servers API
- `GET /api/servers` list servers (paged).
- `GET /api/servers/{id}` get one server.
- `POST /api/servers` create `{ name, hostname?, description? }` → returns `201 Created` with `Location`.
- `DELETE /api/servers/{id}` delete a server and its logs (transactional) → returns `204`.
- `GET /api/servers/{id}/logs` list logs for a server (same filters as `/api/logs`).
- `GET /api/servers/{id}/log-levels` list distinct log levels for that server.
- `POST /api/servers/{id}/logs/reevaluate` recompute levels and optionally merge continuation lines (`merge`, `dryRun`).

Upload logs
- Endpoint: `POST /api/servers/{id}/logs/upload` (`multipart/form-data`).
  - Single file: field `file`.
  - Multiple files: repeat field `files`.
  - Response: `{ "results": [{"file":"name","imported":N}, ...], "total": M }`.
- Parser supports:
  - ISO-8601: `2024-01-15T10:30:45Z INFO com.example.Service - Message`.
  - Log4j-style: `yyyy-MM-dd HH:mm:ss[,SSS] LEVEL source - message`.
  - Syslog-style: `Jan 15 10:30:45 hostname service: Message`.
  - Nginx access lines: HTTP status extracted as level (e.g., `200`, `404`, `500`).
- Continuation lines: non-header lines are appended to previous entry; Postgres-like multi-line statements are handled.
- Limits (configurable in `application.yml`): `max-file-size: 50MB`, `max-request-size: 200MB`.

Tip: to test frontend logs just create a server and upload the nginx-fake.log file.

UI routes
- `/` Dashboard (cards + recent logs + level chart)
- `/logs` Search & browse logs
- `/servers` Manage servers
- `/upload` Upload log files
- `/create` Create a single log entry

Data model
- `LogEntry`: `id`, `timestamp`, `logLevel`, `source`, `message (TEXT)`, `category`, `server(FK)`; indexes on timestamp, log level, source, server.
- `Server`: `id`, `name` (unique), `hostname`, `description`, `createdAt`.

Paging JSON and errors
- Page serialization uses Spring Data’s stable DTO mode to keep JSON shape predictable.
- Client aborts (e.g., closing a tab) are handled gracefully and no longer logged as 500 errors.

Flyway migrations
- `V2__drop_status_and_username_from_log_entry.sql` – clean up legacy columns.
- `V3__backfill_postgres_levels.sql` – normalize log level values from PostgreSQL logs.
- `V4__message_to_text.sql` – widen message column to `TEXT`.

Testing the API
- Curl: see `scripts/curl-examples.sh` (uses `jq` if available).
- Postman: import `postman/Logbook.postman_collection.json` and set base to `http://localhost:8080`.

Troubleshooting
- 413 on upload: increase `spring.servlet.multipart.max-file-size` / `max-request-size` in `application.yml`.
