package com.example.logbook.web;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.domain.LogLevel;
import com.example.logbook.service.LogEntryService;
import com.example.logbook.repository.LogEntryRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
public class LogEntryController {

    private final LogEntryService service;
    private final LogEntryRepository repository;

    public LogEntryController(LogEntryService service, LogEntryRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping
    public Page<LogEntry> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) LogLevel level,
            @RequestParam(required = false) String source,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return service.search(from, to, level, source, query, pageable);
    }

    @GetMapping("/{id}")
    public LogEntry get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    public LogEntry create(@Valid @RequestBody LogEntry entry) {
        return service.create(entry);
    }

    @PutMapping("/{id}")
    public LogEntry update(@PathVariable long id, @Valid @RequestBody LogEntry entry) {
        return service.update(id, entry);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        service.delete(id);
    }

    @GetMapping(value = "/export", produces = { MediaType.TEXT_PLAIN_VALUE, "text/csv", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) LogLevel level,
            @RequestParam(required = false) String source,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(defaultValue = "1000") int limit
    ) {
        // Export first N items using same filter, unsliced by page
        Page<LogEntry> page = service.search(from, to, level, source, query, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp")));
        List<LogEntry> items = page.getContent();

        if ("json".equalsIgnoreCase(format)) {
            String json = items.stream()
                    .map(e -> String.format("{\"id\":%d,\"timestamp\":\"%s\",\"logLevel\":\"%s\",\"source\":\"%s\",\"message\":\"%s\",\"category\":\"%s\"}",
                            e.getId(), e.getTimestamp(), e.getLogLevel(), escape(e.getSource()), escape(e.getMessage()),
                            escape(nz(e.getCategory()))))
                    .collect(Collectors.joining(",","[", "]"));
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=logs.json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        }

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
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=logs.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    @GetMapping("/levels")
    public List<LogLevel> availableLevels() {
        return repository.findDistinctLevels();
    }

    private static String escape(String in) {
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
