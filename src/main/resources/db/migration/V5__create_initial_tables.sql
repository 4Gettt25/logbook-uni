CREATE TABLE IF NOT EXISTS server (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    hostname    VARCHAR(255),
    description VARCHAR(1000),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS log_entry (
    id         BIGSERIAL PRIMARY KEY,
    timestamp  TIMESTAMP WITH TIME ZONE NOT NULL,
    log_level  VARCHAR(50) NOT NULL,
    source     VARCHAR(255) NOT NULL,
    message    TEXT NOT NULL,
    category   VARCHAR(255),
    server_id  BIGINT,
    CONSTRAINT fk_log_entry_server
        FOREIGN KEY (server_id)
        REFERENCES server (id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_log_entry_timestamp ON log_entry (timestamp);
CREATE INDEX IF NOT EXISTS idx_log_entry_loglevel ON log_entry (log_level);
CREATE INDEX IF NOT EXISTS idx_log_entry_source ON log_entry (source);
CREATE INDEX IF NOT EXISTS idx_log_entry_server ON log_entry (server_id);

