package com.example.logbook.web;    

import com.example.logbook.domain.LogEntry;
import com.example.logbook.service.LogEntryService;
import com.example.logbook.repository.LogEntryRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LogEntryController {

    private final LogEntryService service;
    private final LogEntryRepository repository;
    private final Validator validator;

    public LogEntryController(LogEntryService service, LogEntryRepository repository) {
        this.service = service;
        this.repository = repository;
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }
    
    public void registerRoutes(Javalin app) {
        app.get("/api/logs", this::search);
        app.get("/api/logs/{id}", this::get);
        app.post("/api/logs", this::create);
        app.put("/api/logs/{id}", this::update);
        app.delete("/api/logs/{id}", this::delete);
        app.get("/api/logs/export", this::export);
        app.get("/api/logs/levels", this::availableLevels);
    }

    private void search(Context ctx) {
        try {
            Instant from = parseInstant(ctx.queryParam("from"));
            Instant to = parseInstant(ctx.queryParam("to"));
            List<String> levels = ctx.queryParams("level");
            String source = ctx.queryParam("source");
            String query = ctx.queryParam("q");
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(0);
            int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(20);

            LogEntryRepository.PageResult<LogEntry> result = service.search(from, to, levels, source, query, page, size);
            ctx.json(result);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private void get(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            LogEntry entry = service.get(id);
            ctx.json(entry);
        } catch (Exception e) {
            ctx.status(404).json(Map.of("error", e.getMessage()));
        }
    }

    private void create(Context ctx) {
        try {
            LogEntry entry = ctx.bodyAsClass(LogEntry.class);
            validateEntry(entry);
            LogEntry created = service.create(entry);
            ctx.status(201).json(created);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        }
    }

    private void update(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            LogEntry entry = ctx.bodyAsClass(LogEntry.class);
            validateEntry(entry);
            LogEntry updated = service.update(id, entry);
            ctx.json(updated);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        }
    }

    private void delete(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            service.delete(id);
            ctx.status(204);
        } catch (Exception e) {
            ctx.status(404).json(Map.of("error", e.getMessage()));
        }
    }

    private void export(Context ctx) {
        try {
            Instant from = parseInstant(ctx.queryParam("from"));
            Instant to = parseInstant(ctx.queryParam("to"));
            List<String> levels = ctx.queryParams("level");
            String source = ctx.queryParam("source");
            String query = ctx.queryParam("q");
            String format = ctx.queryParam("format");
            if (format == null) format = "csv";
            int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(1000);

            LogEntryRepository.PageResult<LogEntry> page = service.search(from, to, levels, source, query, 0, limit);
            List<LogEntry> items = page.getContent();

            if ("json".equalsIgnoreCase(format)) {
                String json = items.stream()
                        .map(e -> String.format("{\"id\":%d,\"timestamp\":\"%s\",\"logLevel\":\"%s\",\"source\":\"%s\",\"message\":\"%s\",\"category\":\"%s\"}",
                                e.getId(), e.getTimestamp(), e.getLogLevel(), escape(e.getSource()), escape(e.getMessage()),
                                escape(nz(e.getCategory()))))
                        .collect(Collectors.joining(",","[", "]"));
                
                ctx.header("Content-Disposition", "attachment; filename=logs.json")
                   .contentType("application/json")
                   .result(json.getBytes(StandardCharsets.UTF_8));
            } else {
                // default CSV
                String header = "id,timestamp,level,source,message,category\n";
                String csv = items.stream()
                        .map(e -> String.join(",",
                                String.valueOf(e.getId()),
                                safe(e.getTimestamp()),
                                safe(e.getLogLevel()),
                                csvField(e.getSource()),
                                csvField(e.getMessage()),
                                csvField(e.getCategory())))
                        .collect(Collectors.joining("\n", header, "\n"));
                
                ctx.header("Content-Disposition", "attachment; filename=logs.csv")
                   .contentType("text/csv")
                   .result(csv.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private void availableLevels(Context ctx) {
        try {
            List<String> levels = repository.findDistinctLevels();
            levels.sort(String.CASE_INSENSITIVE_ORDER);
            ctx.json(levels);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }
    
    private void validateEntry(LogEntry entry) {
        Set<ConstraintViolation<LogEntry>> violations = validator.validate(entry);
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
    }    private static String escape(String in) {
        return in.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String nz(String v) { return v == null ? "" : v; }
    private static String safe(Object v) { return v == null ? "" : String.valueOf(v); }
    private static String csvField(String v) {
        String s = nz(v);
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
