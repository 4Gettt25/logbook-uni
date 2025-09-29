package com.example.logbook.web;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.domain.Server;
import com.example.logbook.repository.LogEntryRepository;
import com.example.logbook.service.LogEntryService;
import com.example.logbook.service.LogImportService;
import com.example.logbook.service.LogMaintenanceService;
import com.example.logbook.service.ServerService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerController {

    private static final String SERVER_SORT_PATTERN = "^(id|name|hostname|createdAt)$";

    private final ServerService servers;
    private final LogEntryService logService;
    private final LogImportService importService;
    private final LogMaintenanceService maintenance;
    private final Validator validator;

    public ServerController(ServerService servers, LogEntryService logService, LogImportService importService,
                            LogMaintenanceService maintenance) {
        this.servers = servers;
        this.logService = logService;
        this.importService = importService;
        this.maintenance = maintenance;
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/servers", this::list);
        app.get("/api/servers/{id}", this::get);
        app.post("/api/servers", this::create);
        app.delete("/api/servers/{id}", this::delete);
        app.get("/api/servers/{id}/logs", this::logs);
        app.get("/api/servers/{id}/log-levels", this::availableLevels);
        app.post("/api/servers/{id}/logs/reevaluate", this::reevaluate);
        app.post("/api/servers/{id}/logs/upload", this::upload);
    }

    private void list(Context ctx) {
        try {
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(0);
            int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(20);
            String sort = ctx.queryParam("sort");
            if (sort == null || sort.isBlank() || !sort.matches(SERVER_SORT_PATTERN)) {
                sort = "name";
            }
            boolean desc = ctx.queryParamAsClass("desc", Boolean.class).getOrDefault(false);

            LogEntryRepository.PageResult<Server> result = servers.list(page, size, sort, desc);
            ctx.json(result);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private void get(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Server server = servers.get(id);
            ctx.json(server);
        } catch (NoSuchElementException e) {
            ctx.status(404).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private void create(Context ctx) {
        try {
            Server server = ctx.bodyAsClass(Server.class);
            validateServer(server);
            Server saved = servers.create(server);
            ctx.status(201)
               .header("Location", "/api/servers/" + saved.getId())
               .json(saved);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private void delete(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            servers.delete(id);
            ctx.status(204);
        } catch (NoSuchElementException e) {
            ctx.status(404).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private void logs(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Instant from = parseInstant(ctx.queryParam("from"));
            Instant to = parseInstant(ctx.queryParam("to"));
            List<String> levels = ctx.queryParams("level");
            String source = ctx.queryParam("source");
            String query = ctx.queryParam("q");
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(0);
            int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(20);

            servers.get(id); // ensure server exists
            LogEntryRepository.PageResult<LogEntry> result = logService.searchByServer(id, from, to, levels, source, query, page, size);
            ctx.json(result);
        } catch (NoSuchElementException e) {
            ctx.status(404).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private void availableLevels(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            List<String> levels = servers.listLogLevels(id);
            ctx.json(levels);
        } catch (NoSuchElementException e) {
            ctx.status(404).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private void reevaluate(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            boolean merge = ctx.queryParamAsClass("merge", Boolean.class).getOrDefault(false);
            boolean dryRun = ctx.queryParamAsClass("dryRun", Boolean.class).getOrDefault(false);

            servers.get(id); // ensure server exists
            var result = maintenance.reevaluateServer(id, merge, dryRun);
            ctx.json(result);
        } catch (NoSuchElementException e) {
            ctx.status(404).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private void upload(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Server server = servers.get(id);

            List<UploadFileResult> results = new ArrayList<>();

            UploadedFile file = ctx.uploadedFile("file");
            if (file != null) {
                byte[] content = file.content().readAllBytes();
                int count = importService.importText(content, server);
                results.add(new UploadFileResult(file.filename(), count));
            }

            List<UploadedFile> files = ctx.uploadedFiles("files");
            if (files != null) {
                for (UploadedFile f : files) {
                    if (f != null) {
                        byte[] content = f.content().readAllBytes();
                        int count = importService.importText(content, server);
                        results.add(new UploadFileResult(f.filename(), count));
                    }
                }
            }

            int total = results.stream().mapToInt(UploadFileResult::imported).sum();
            ctx.json(new UploadResults(results, total));
        } catch (NoSuchElementException e) {
            ctx.status(404).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        }
    }

    private void validateServer(Server server) {
        Set<ConstraintViolation<Server>> violations = validator.validate(server);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Validation failed: " + errors);
        }
    }

    private Instant parseInstant(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timestamp format: " + value);
        }
    }

    public record UploadFileResult(String file, int imported) {}
    public record UploadResults(List<UploadFileResult> results, int total) {}
}
