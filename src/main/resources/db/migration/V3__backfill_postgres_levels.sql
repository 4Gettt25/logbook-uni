-- Backfill misclassified Postgres log levels that were imported as milliseconds or defaults
-- Note: This is simplified for H2 compatibility. PostgreSQL version would use regexp_matches.

-- Simple pattern matching for common log levels at the start of messages
UPDATE log_entry
SET log_level = CASE
    WHEN upper(message) LIKE 'ERROR:%' THEN 'ERROR'
    WHEN upper(message) LIKE 'FATAL:%' THEN 'FATAL'  
    WHEN upper(message) LIKE 'PANIC:%' THEN 'FATAL'
    WHEN upper(message) LIKE 'WARNING:%' THEN 'WARN'
    WHEN upper(message) LIKE 'WARN:%' THEN 'WARN'
    WHEN upper(message) LIKE 'INFO:%' THEN 'INFO'
    WHEN upper(message) LIKE 'LOG:%' THEN 'LOG'
    WHEN upper(message) LIKE 'DEBUG%:%' THEN 'DEBUG'
    WHEN upper(message) LIKE 'STATEMENT:%' THEN 'LOG'
    WHEN upper(message) LIKE 'DETAIL:%' THEN 'LOG'
    WHEN upper(message) LIKE 'HINT:%' THEN 'LOG'
    WHEN upper(message) LIKE 'CONTEXT:%' THEN 'LOG'
    WHEN upper(message) LIKE 'NOTICE:%' THEN 'LOG'
END
WHERE log_level IS NOT NULL
  AND upper(message) NOT LIKE '%HTTP/%'  -- exclude HTTP access logs
  AND (
      log_level LIKE '___'      -- previously misparsed numeric like 112/496 (3-digit numbers)
      OR upper(log_level) IN ('INFO','WARN','WARNING','DEBUG','LOG')
  );

