package com.example.logbook.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.Profiles;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);
    private static final Path H2_DB_FILE = Paths.get("data", "logbook.mv.db");
    private static final Path H2_TRACE_FILE = Paths.get("data", "logbook.trace.db");

    private final ConfigurableEnvironment environment;

    public DataSourceConfig(ConfigurableEnvironment environment) {
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        boolean postgresProfile = environment.acceptsProfiles(Profiles.of("postgres"));
        if (postgresProfile) {
            try {
                DataSource postgres = createPostgresDataSource();
                overrideJpaDialect("org.hibernate.dialect.PostgreSQLDialect");
                overrideFlywayEnabled(true);
                return postgres;
            } catch (Exception ex) {
                log.warn("PostgreSQL datasource unavailable, falling back to embedded H2: {}", ex.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("PostgreSQL fallback stacktrace", ex);
                }
                overrideFlywayEnabled(false);
            }
        }

        DataSource fallback = createEmbeddedH2DataSource();
        overrideJpaDialect("org.hibernate.dialect.H2Dialect");
        overrideFlywayEnabled(false);
        return fallback;
    }

    private DataSource createPostgresDataSource() throws SQLException {
        String host = getEnv("DB_HOST", "localhost");
        String port = getEnv("DB_PORT", "5432");
        String dbName = getEnv("DB_NAME", "logbook");
        String maintenanceDb = getEnv("DB_MAINTENANCE_DB", "postgres");
        String username = getEnv("DB_USER", "postgres");
        String password = getEnv("DB_PASSWORD", "");

        ensurePostgresDatabaseExists(host, port, maintenanceDb, dbName, username, password);

        String jdbcUrl = String.format(Locale.ROOT, "jdbc:postgresql://%s:%s/%s", host, port, dbName);
        log.info("[database] Using PostgreSQL datasource at {}", jdbcUrl);

        HikariConfig config = new HikariConfig();
        config.setPoolName("logbook-postgres");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(getEnvAsInt("DB_POOL_MAX", 10));
        config.setMinimumIdle(getEnvAsInt("DB_POOL_MIN", 2));
        config.setConnectionTimeout(Duration.ofSeconds(30).toMillis());
        config.setInitializationFailTimeout(Duration.ofSeconds(5).toMillis());

        return new HikariDataSource(config);
    }

    private void ensurePostgresDatabaseExists(String host, String port, String maintenanceDb,
                                              String databaseName, String username, String password) throws SQLException {
        String maintenanceUrl = String.format(Locale.ROOT, "jdbc:postgresql://%s:%s/%s", host, port, maintenanceDb);
        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);

        try (Connection conn = DriverManager.getConnection(maintenanceUrl, props)) {
            if (!databaseExists(conn, databaseName)) {
                createDatabase(conn, databaseName);
                log.info("[database] Created PostgreSQL database '{}'", databaseName);
            }
        }
    }

    private boolean databaseExists(Connection conn, String dbName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
            stmt.setString(1, dbName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void createDatabase(Connection conn, String dbName) throws SQLException {
        String escapedName = dbName.replace("\"", "\"\"");
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE \"" + escapedName + "\"");
        }
    }

    private DataSource createEmbeddedH2DataSource() {
        String url = environment.getProperty("spring.datasource.url", "jdbc:h2:file:./data/logbook;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        String username = environment.getProperty("spring.datasource.username", "sa");
        String password = environment.getProperty("spring.datasource.password", "");

        log.info("[database] Using embedded H2 datasource at {}", url);

        ensureH2FilesAreAvailable(url, username, password);

        HikariConfig config = new HikariConfig();
        config.setPoolName("logbook-h2");
        config.setJdbcUrl(url);
        config.setDriverClassName("org.h2.Driver");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        return new HikariDataSource(config);
    }

    private void ensureH2FilesAreAvailable(String url, String username, String password) {
        try {
            Files.createDirectories(H2_DB_FILE.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create data directory for H2 database", e);
        }

        try (Connection ignored = DriverManager.getConnection(url, username, password)) {
            // Existing database opened successfully.
        } catch (SQLException e) {
            if (e.getErrorCode() == 90020) {
                handleStaleH2Files();
                try (Connection ignored = DriverManager.getConnection(url, username, password)) {
                    // New database created after cleanup.
                } catch (SQLException retry) {
                    throw new IllegalStateException("Failed to prepare embedded H2 database after cleanup", retry);
                }
            } else {
                throw new IllegalStateException("Failed to prepare embedded H2 database", e);
            }
        }
    }

    private void handleStaleH2Files() {
        log.warn("[database] Detected stale H2 lock. Removing old database files (data will be reset).");
        try {
            Files.deleteIfExists(H2_DB_FILE);
            Files.deleteIfExists(H2_TRACE_FILE);
        } catch (FileSystemException e) {
            throw new IllegalStateException("H2 database files are locked by another process. Please stop other instances or reboot.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to clean up stale H2 database files", e);
        }
    }

    private void overrideJpaDialect(String dialect) {
        MutablePropertySources sources = environment.getPropertySources();
        Map<String, Object> props = Map.of("spring.jpa.properties.hibernate.dialect", dialect);
        if (sources.contains("dynamicJpaDialect")) {
            sources.replace("dynamicJpaDialect", new MapPropertySource("dynamicJpaDialect", props));
        } else {
            sources.addFirst(new MapPropertySource("dynamicJpaDialect", props));
        }
    }

    private void overrideFlywayEnabled(boolean enabled) {
        MutablePropertySources sources = environment.getPropertySources();
        Map<String, Object> props = Map.of("spring.flyway.enabled", enabled);
        if (sources.contains("dynamicFlywayToggle")) {
            sources.replace("dynamicFlywayToggle", new MapPropertySource("dynamicFlywayToggle", props));
        } else {
            sources.addFirst(new MapPropertySource("dynamicFlywayToggle", props));
        }
    }

    private String getEnv(String key, String defaultValue) {
        String value = environment.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private int getEnvAsInt(String key, int defaultValue) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            log.warn("Invalid integer for {}: '{}' - using {}", key, value, defaultValue);
            return defaultValue;
        }
    }
}
