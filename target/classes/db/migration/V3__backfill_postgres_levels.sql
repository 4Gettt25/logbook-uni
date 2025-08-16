-- Backfill misclassified Postgres log levels that were imported as milliseconds or defaults
-- Only updates rows whose message contains a Postgres-style severity token and are not HTTP access logs

-- Normalize level from message tokens like: ERROR:, FATAL:, WARNING:, LOG:, DEBUG1:, STATEMENT:, DETAIL:, etc.
-- Map WARNING -> WARN, DEBUG* -> DEBUG, STATEMENT/DETAIL/HINT/CONTEXT -> LOG, PANIC -> FATAL, NOTICE -> LOG

UPDATE log_entry le
SET log_level = CASE
    WHEN upper(m.sev) LIKE 'DEBUG%' THEN 'DEBUG'
    WHEN upper(m.sev) = 'WARNING' THEN 'WARN'
    WHEN upper(m.sev) IN ('STATEMENT','DETAIL','HINT','CONTEXT') THEN 'LOG'
    WHEN upper(m.sev) = 'PANIC' THEN 'FATAL'
    WHEN upper(m.sev) = 'NOTICE' THEN 'LOG'
    ELSE upper(m.sev)
END
FROM (
    SELECT id,
           (regexp_matches(message, '(ERROR|FATAL|PANIC|WARNING|WARN|NOTICE|INFO|LOG|DEBUG[0-5]?|STATEMENT|DETAIL|HINT|CONTEXT)\s*:', 'i'))[1] AS sev
    FROM log_entry
) AS m
WHERE le.id = m.id
  AND m.sev IS NOT NULL
  AND le.message !~* 'HTTP/\d\.\d"'  -- exclude nginx/access-style HTTP lines
  AND (
      le.log_level ~ '^[0-9]{3}$'      -- previously misparsed numeric like 112/496
      OR upper(le.log_level) IN ('INFO','WARN','WARNING','DEBUG','LOG')
  );

