package com.example.logbook;

import com.example.logbook.config.DatabaseConfig;
import com.example.logbook.config.ThymeleafConfig;
import com.example.logbook.repository.LogEntryRepository;
import com.example.logbook.repository.ServerRepository;
import com.example.logbook.service.LogEntryService;
import com.example.logbook.service.LogImportService;
import com.example.logbook.service.LogMaintenanceService;
import com.example.logbook.web.LogEntryController;
import com.example.logbook.web.ServerController;
import com.example.logbook.web.WebController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;

public class LogbookApplication {
    
    public static void main(String[] args) {
        // Load environment variables
        Dotenv dotenv = Dotenv.configure()
                .filename(".env")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();
        
        // Set up database
        DatabaseConfig dbConfig = new DatabaseConfig(dotenv);
        EntityManagerFactory emf = dbConfig.createEntityManagerFactory();
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        
        // Run Flyway migrations
        Flyway flyway = dbConfig.createFlyway();
        flyway.migrate();
        
        // Configure Jackson for JSON
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Set up Thymeleaf
        ThymeleafConfig thymeleafConfig = new ThymeleafConfig();
        
        // Create repositories
        LogEntryRepository logEntryRepository = new LogEntryRepository(sessionFactory);
        ServerRepository serverRepository = new ServerRepository(sessionFactory);
        
        // Create services
        LogEntryService logEntryService = new LogEntryService(logEntryRepository, serverRepository);
        LogImportService logImportService = new LogImportService(logEntryRepository, serverRepository);
        LogMaintenanceService logMaintenanceService = new LogMaintenanceService(logEntryRepository);
        
        // Create controllers
        WebController webController = new WebController(thymeleafConfig.getTemplateEngine());
        LogEntryController logEntryController = new LogEntryController(logEntryService, logEntryRepository);
        ServerController serverController = new ServerController(serverRepository, logEntryService, 
                logImportService, logEntryRepository, logMaintenanceService);
        
        // Create and configure Javalin app
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/static", Location.CLASSPATH);
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });
        
        // Register routes
        webController.registerRoutes(app);
        logEntryController.registerRoutes(app);
        serverController.registerRoutes(app);
        
        // Start server
        int port = Integer.parseInt(dotenv.get("PORT", "8080"));
        app.start(port);
        
        System.out.println("Logbook application started on port " + port);
        
        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            emf.close();
        }));
    }
}

