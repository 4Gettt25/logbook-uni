package com.example.logbook.config;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.domain.Server;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import javax.sql.DataSource;
import java.util.Properties;

public class DatabaseConfig {
    
    private final Dotenv dotenv;
    private final boolean isPostgres;
    
    public DatabaseConfig(Dotenv dotenv) {
        this.dotenv = dotenv;
        String profile = dotenv.get("SPRING_PROFILES_ACTIVE", "");
        this.isPostgres = "postgres".equals(profile);
    }
    
    public EntityManagerFactory createEntityManagerFactory() {
        Configuration configuration = new Configuration();
        
        // Add annotated classes
        configuration.addAnnotatedClass(LogEntry.class);
        configuration.addAnnotatedClass(Server.class);
        
        // Set properties
        Properties props = getHibernateProperties();
        configuration.setProperties(props);
        
        // Create SessionFactory and wrap as EntityManagerFactory
        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties());
        
        SessionFactory sessionFactory = configuration.buildSessionFactory(builder.build());
        return sessionFactory.unwrap(EntityManagerFactory.class);
    }
    
    private Properties getHibernateProperties() {
        Properties props = new Properties();
        
        if (isPostgres) {
            // PostgreSQL configuration
            String host = dotenv.get("DB_HOST", "localhost");
            String port = dotenv.get("DB_PORT", "5432");
            String dbName = dotenv.get("DB_NAME", "logbook");
            String username = dotenv.get("DB_USER", "postgres");
            String password = dotenv.get("DB_PASSWORD", "");
            
            props.setProperty("hibernate.connection.url", 
                String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName));
            props.setProperty("hibernate.connection.username", username);
            props.setProperty("hibernate.connection.password", password);
            props.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
            props.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        } else {
            // H2 configuration (default)
            props.setProperty("hibernate.connection.url", 
                "jdbc:h2:file:./data/logbook;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
            props.setProperty("hibernate.connection.username", "sa");
            props.setProperty("hibernate.connection.password", "");
            props.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        }
        
        // Common Hibernate properties
        props.setProperty("hibernate.hbm2ddl.auto", "update");
        props.setProperty("hibernate.format_sql", "true");
        props.setProperty("hibernate.connection.provider_class", 
            "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
        props.setProperty("hibernate.hikari.maximumPoolSize", "10");
        props.setProperty("hibernate.hikari.minimumIdle", "2");
        
        return props;
    }
    
    public Flyway createFlyway() {
        DataSource dataSource = createDataSource();
        return Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .load();
    }
    
    private DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        
        if (isPostgres) {
            String host = dotenv.get("DB_HOST", "localhost");
            String port = dotenv.get("DB_PORT", "5432");
            String dbName = dotenv.get("DB_NAME", "logbook");
            String username = dotenv.get("DB_USER", "postgres");
            String password = dotenv.get("DB_PASSWORD", "");
            
            config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName));
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.postgresql.Driver");
        } else {
            config.setJdbcUrl("jdbc:h2:file:./data/logbook;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
            config.setUsername("sa");
            config.setPassword("");
            config.setDriverClassName("org.h2.Driver");
        }
        
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        
        return new HikariDataSource(config);
    }
}
