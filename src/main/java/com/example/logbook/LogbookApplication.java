package com.example.logbook;

import com.example.logbook.config.DatabaseConfig;
import com.example.logbook.config.ThymeleafConfig;
import com.example.logbook.repository.LogEntryRepository;
import com.example.logbook.repository.ServerRepository;
import com.example.logbook.service.LogEntryService;
import com.example.logbook.service.LogImportService;
import com.example.logbook.service.LogMaintenanceService;
import com.example.logbook.service.ServerService;
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
import org.hibernate.SessionFactory;

public class LogbookApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .filename(".env")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        DatabaseConfig dbConfig = new DatabaseConfig(dotenv);
        dbConfig.runMigrations();
        EntityManagerFactory emf = dbConfig.createEntityManagerFactory();
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        ThymeleafConfig thymeleafConfig = new ThymeleafConfig();

        LogEntryRepository logEntryRepository = new LogEntryRepository(sessionFactory);
        ServerRepository serverRepository = new ServerRepository(sessionFactory);

        LogEntryService logEntryService = new LogEntryService(logEntryRepository);
        LogImportService logImportService = new LogImportService(logEntryRepository, serverRepository);
        LogMaintenanceService logMaintenanceService = new LogMaintenanceService(logEntryRepository);
        ServerService serverService = new ServerService(serverRepository, logEntryRepository, sessionFactory);

        WebController webController = new WebController(thymeleafConfig.getTemplateEngine());
        LogEntryController logEntryController = new LogEntryController(logEntryService, logEntryRepository);
        ServerController serverController = new ServerController(serverService, logEntryService,
                logImportService, logMaintenanceService);

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/static", Location.CLASSPATH);
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        webController.registerRoutes(app);
        logEntryController.registerRoutes(app);
        serverController.registerRoutes(app);

        int port = Integer.parseInt(dotenv.get("PORT", "8080"));
        app.start(port);

        System.out.println("Logbook application started on port " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            emf.close();
        }));
    }
}
