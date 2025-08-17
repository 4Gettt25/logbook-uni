-- Allow large log messages (no 4k limit)
-- PostgreSQL: use TEXT; H2 accepts TEXT as alias
ALTER TABLE log_entry ALTER COLUMN message TYPE TEXT;

