package com.example.logbook.config;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.domain.Server;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.h2.jdbc.JdbcSQLNonTransientConnectionException;

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
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

public class DatabaseConfig {

    private static final String H2_URL_BASE = "jdbc:h2:file:./data/logbook";
    private static final String H2_URL = H2_URL_BASE + ";AUTO_SERVER=TRUE;AUTO_SERVER_PORT=0;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

    private static final Path H2_DB_FILE = Paths.get("data", "logbook.mv.db");
    private static final Path H2_TRACE_FILE = Paths.get("data", "logbook.trace.db");

    private final Dotenv dotenv;
    private final boolean usePostgres;

    public DatabaseConfig(Dotenv dotenv) {
        this.dotenv = Objects.requireNonNull(dotenv, "dotenv");
        this.usePostgres = resolveUsePostgres();

        if (usePostgres) {
            System.out.println("[database] Using PostgreSQL connection settings");
            ensurePostgresDatabaseExists();
        } else {
            System.out.println("[database] Using embedded H2 database");
            ensureH2FilesAreAvailable();
        }
    }

    public void runMigrations() {
        try (HikariDataSource dataSource = createDataSource()) {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .baselineOnMigrate(true)
                    .load();

            try {
                flyway.migrate();
            } catch (FlywayValidateException validationException) {
                System.out.println("[database] Flyway validation failed: " + validationException.getMessage());
                System.out.println("[database] Attempting Flyway repair to align schema history with local migrations...");
                flyway.repair();
                flyway.migrate();
            }
        }
    }

    public EntityManagerFactory createEntityManagerFactory() {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(LogEntry.class);
        configuration.addAnnotatedClass(Server.class);
        configuration.setProperties(buildHibernateProperties());

        StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties());

        SessionFactory sessionFactory = configuration.buildSessionFactory(registryBuilder.build());
        return sessionFactory;
    }

    private Properties buildHibernateProperties() {
        Properties props = new Properties();

        if (usePostgres) {
            props.setProperty("hibernate.connection.url", resolvePostgresUrl());
            props.setProperty("hibernate.connection.username", getenv("DB_USER", "postgres"));
            props.setProperty("hibernate.connection.password", getenv("DB_PASSWORD", ""));
            props.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
            props.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            props.setProperty("hibernate.hbm2ddl.auto", "none");
        } else {
            props.setProperty("hibernate.connection.url", H2_URL);
            props.setProperty("hibernate.connection.username", "sa");
            props.setProperty("hibernate.connection.password", "");
            props.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            props.setProperty("hibernate.hbm2ddl.auto", "update");
        }

        props.setProperty("hibernate.format_sql", "true");
        props.setProperty("hibernate.current_session_context_class", "managed");
        props.setProperty("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
        props.setProperty("hibernate.hikari.maximumPoolSize", getenv("DB_POOL_MAX", "10"));
        props.setProperty("hibernate.hikari.minimumIdle", getenv("DB_POOL_MIN", "2"));

        return props;
    }

    private HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();

        if (usePostgres) {
            config.setJdbcUrl(resolvePostgresUrl());
            config.setUsername(getenv("DB_USER", "postgres"));
            config.setPassword(getenv("DB_PASSWORD", ""));
            config.setDriverClassName("org.postgresql.Driver");
        } else {
            config.setJdbcUrl(H2_URL);
            config.setUsername("sa");
            config.setPassword("");
            config.setDriverClassName("org.h2.Driver");
        }

        config.setMaximumPoolSize(Integer.parseInt(getenv("DB_POOL_MAX", "10")));
        config.setMinimumIdle(Integer.parseInt(getenv("DB_POOL_MIN", "2")));
        config.setPoolName("logbook-pool");
        config.setAutoCommit(true);
        config.setInitializationFailTimeout(-1);

        return new HikariDataSource(config);
    }

    private boolean resolveUsePostgres() {
        String explicitFlag = trimToNull(dotenv.get("DB_USE_POSTGRES"));
        if (explicitFlag != null) {
            return Boolean.parseBoolean(explicitFlag);
        }

        if (trimToNull(dotenv.get("DATABASE_URL")) != null || trimToNull(dotenv.get("DB_URL")) != null) {
            return true;
        }

        boolean hasHost = trimToNull(dotenv.get("DB_HOST")) != null;
        boolean hasUser = trimToNull(dotenv.get("DB_USER")) != null;
        return hasHost && hasUser;
    }

    private void ensurePostgresDatabaseExists() {
        String host = getenv("DB_HOST", "localhost");
        String port = getenv("DB_PORT", "5432");
        String dbName = getenv("DB_NAME", "logbook");
        String maintenanceDb = getenv("DB_MAINTENANCE_DB", "postgres");
        String username = getenv("DB_USER", "postgres");
        String password = getenv("DB_PASSWORD", "");

        String maintenanceUrl = String.format(Locale.ROOT, "jdbc:postgresql://%s:%s/%s", host, port, maintenanceDb);

        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);

        try (Connection conn = DriverManager.getConnection(maintenanceUrl, props)) {
            if (!databaseExists(conn, dbName)) {
                createDatabase(conn, dbName);
                System.out.println("[database] Created PostgreSQL database '" + dbName + "'");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to ensure PostgreSQL database '" + dbName + "' exists", e);
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

    private String resolvePostgresUrl() {
        String directUrl = trimToNull(dotenv.get("DATABASE_URL"));
        if (directUrl == null) {
            directUrl = trimToNull(dotenv.get("DB_URL"));
        }
        if (directUrl != null) {
            return directUrl;
        }

        String host = getenv("DB_HOST", "localhost");
        String port = getenv("DB_PORT", "5432");
        String dbName = getenv("DB_NAME", "logbook");
        return String.format(Locale.ROOT, "jdbc:postgresql://%s:%s/%s", host, port, dbName);
    }

    private void ensureH2FilesAreAvailable() {
        try {
            Files.createDirectories(H2_DB_FILE.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create data directory for H2 database", e);
        }

        if (Files.notExists(H2_DB_FILE)) {
            return;
        }

        try (Connection ignored = DriverManager.getConnection(H2_URL, "sa", "")) {
            // Existing database opened successfully.
        } catch (JdbcSQLNonTransientConnectionException e) {
            if (e.getErrorCode() == 90020) {
                handleStaleH2Files();
            } else {
                throw new IllegalStateException("Failed to prepare embedded H2 database", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to prepare embedded H2 database", e);
        }
    }

    private void handleStaleH2Files() {
        System.out.println("[database] Detected stale H2 lock. Removing old database files (data will be reset).");
        try {
            Files.deleteIfExists(H2_DB_FILE);
            Files.deleteIfExists(H2_TRACE_FILE);
        } catch (FileSystemException e) {
            throw new IllegalStateException("H2 database files are locked by another process. Please stop other instances or reboot.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to clean up stale H2 database files", e);
        }
    }

    private String getenv(String key, String defaultValue) {
        String value = trimToNull(dotenv.get(key));
        return value != null ? value : defaultValue;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}





