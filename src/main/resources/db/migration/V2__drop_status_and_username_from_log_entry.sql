-- Drop obsolete columns after removing them from the entity
-- Works for PostgreSQL and H2
ALTER TABLE log_entry DROP COLUMN IF EXISTS status;
ALTER TABLE log_entry DROP COLUMN IF EXISTS username;

