Spring Logbook (Maven, Spring Boot)

How to run
- Prereqs: Java 17, Maven 3.9+.
- Dev (H2 persistent): `mvn spring-boot:run`
- Tests: `mvn test`
- Package: `mvn clean package`

Database configuration
- Default profile uses a file-based H2 database so data persists across restarts.
  - Files are stored under `./data/logbook.*` (ignored by Git).
  - H2 console: `/h2-console` (Driver: `org.h2.Driver`, JDBC URL: `jdbc:h2:file:./data/logbook`).
- To use PostgreSQL (recommended for shared environments):
  1) Copy `.env.example` to `.env` and set values:
     - `SPRING_PROFILES_ACTIVE=postgres`
     - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
  2) Start the app as usual: `mvn spring-boot:run`
  3) Spring will connect to Postgres and auto-create/update tables (`ddl-auto: update`).
  4) Ensure the Postgres user has permissions to create/alter tables in the target schema.
  
How .env is loaded
- The app imports `.env` early via `spring.config.import`. It supports both:
  - `SPRING_PROFILES_ACTIVE=postgres` (uppercase, .env style) and
  - `spring.profiles.active=postgres` (standard Spring property)
  so the `postgres` profile activates without extra flags.
- Alternatives: set an OS env var or use the Maven flag:
  - `SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run`
  - `mvn spring-boot:run -Dspring-boot.run.profiles=postgres`

Important notes
- Tests use in-memory H2 and explicitly disable `.env` loading, so test runs never require Postgres:
  - See `src/test/resources/application.yml` where `dotenv.enabled=false`.
- Tests also set `spring.config.import` to empty so `.env` is never imported during tests.
- If you want to stick with H2 for local persistence, make sure your `.env` does not set `SPRING_PROFILES_ACTIVE=postgres`.
- If you enable the `postgres` profile via `.env`, that only affects runtime (not tests).

REST API
- `GET /api/logs` with filters: `from`/`to` (ISO-8601), `level`, `source`, `q` (message contains), `status`, `username`, `page`, `size`.
- `GET /api/logs/{id}` fetch one.
- `POST /api/logs` create.
- `PUT /api/logs/{id}` update.
- `DELETE /api/logs/{id}` delete.
- `GET /api/logs/export?format=csv|json` exports filtered set.

Servers
- `GET /api/servers` list servers (paged).
- `GET /api/servers/{id}` get one server.
- `POST /api/servers` create server `{ name, hostname?, description? }`.
- `DELETE /api/servers/{id}` delete a server (returns 204).
- `GET /api/servers/{id}/logs` list logs for a server (supports same filters as `/api/logs`).
- `POST /api/servers/{id}/logs/upload` upload one or more logfiles (`multipart/form-data`).
  - Single file: field `file`.
  - Multiple: repeat field `files` for each file.
  - Response: `{ "results": [{"file":"name","imported":N}, ...], "total": M }`.
  - Parser supports formats:
    - `ISO-8601 TIMESTAMP LEVEL SOURCE - MESSAGE` (e.g., `2024-08-16T12:34:56Z INFO app - started`).
    - Log4j-like: `yyyy-MM-dd HH:mm:ss[,SSS] LEVEL source - message`.
    - Syslog-like: `MMM d HH:mm:ss host source[pid]: message` (assumes current year; level default INFO).

Behavior changes (latest)
- Default DB changed from H2 in-memory to H2 file-based for persistence across restarts.
- POST `/api/servers` now flushes immediately and returns `201 Created` with `Location` header.
- Added `DELETE /api/servers/{id}` to align with the UI.

PostgreSQL profile
- You can use a `.env` file to avoid plain-text config in `application.yml`.
- Copy `.env.example` to `.env` and edit values:
  - `SPRING_PROFILES_ACTIVE=postgres`
  - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- The app loads `.env` automatically (via spring-dotenv). Standard environment variables also work.
- Default dev profile uses in-memory H2.

Notes
- Entity: `LogEntry` with fields `id, timestamp, logLevel, source, message, username, category, status`.
- Entity: `Server` with fields `id, name, hostname, description, createdAt`. `LogEntry` references `Server`.
- Pagination defaults to `size=20`, sorted by `timestamp` desc.

Testing the API
- Curl: see `scripts/curl-examples.sh` (uses `jq` to pretty print JSON if available).
- Postman: import `postman/Logbook.postman_collection.json` and set `baseUrl` to your server (default `http://localhost:8080`).
