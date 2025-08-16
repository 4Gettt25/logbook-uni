package com.example.logbook.web;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.domain.LogLevel;
import com.example.logbook.domain.LogStatus;
import com.example.logbook.domain.Server;
import com.example.logbook.repository.ServerRepository;
import com.example.logbook.service.LogEntryService;
import com.example.logbook.service.LogImportService;
import com.example.logbook.repository.LogEntryRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/servers")
public class ServerController {

    private final ServerRepository servers;
    private final LogEntryService logService;
    private final LogImportService importService;
    private final LogEntryRepository logEntries;

    public ServerController(ServerRepository servers, LogEntryService logService, LogImportService importService, LogEntryRepository logEntries) {
        this.servers = servers;
        this.logService = logService;
        this.importService = importService;
        this.logEntries = logEntries;
    }

    @GetMapping
    public Page<Server> list(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return servers.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")));
    }

    @GetMapping("/{id}")
    public Server get(@PathVariable long id) {
        return servers.findById(id).orElseThrow(() -> new NoSuchElementException("Server not found: " + id));
    }

    @PostMapping
    public ResponseEntity<Server> create(@Valid @RequestBody Server server) {
        Server saved = servers.saveAndFlush(server);
        return ResponseEntity
                .created(java.net.URI.create("/api/servers/" + saved.getId()))
                .body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        if (!servers.existsById(id)) {
            throw new NoSuchElementException("Server not found: " + id);
        }
        // Remove associated logs first to satisfy FK constraints
        logEntries.deleteByServer_Id(id);
        servers.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/logs")
    public Page<LogEntry> logs(@PathVariable long id,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                               @RequestParam(required = false) LogLevel level,
                               @RequestParam(required = false) String source,
                               @RequestParam(required = false, name = "q") String query,
                               @RequestParam(required = false) LogStatus status,
                               @RequestParam(required = false) String username,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        // ensure server exists
        servers.findById(id).orElseThrow(() -> new NoSuchElementException("Server not found: " + id));
        return logService.searchByServer(id, from, to, level, source, query, status, username, pageable);
    }

    @PostMapping(path = "/{id}/logs/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResults> upload(@PathVariable long id,
                                                @RequestPart(value = "file", required = false) MultipartFile file,
                                                @RequestPart(value = "files", required = false) java.util.List<MultipartFile> files) throws IOException {
        Server server = servers.findById(id).orElseThrow(() -> new NoSuchElementException("Server not found: " + id));
        java.util.List<UploadFileResult> results = new java.util.ArrayList<>();
        if (file != null && !file.isEmpty()) {
            int count = importService.importText(file.getBytes(), server);
            results.add(new UploadFileResult(file.getOriginalFilename(), count));
        }
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    int count = importService.importText(f.getBytes(), server);
                    results.add(new UploadFileResult(f.getOriginalFilename(), count));
                }
            }
        }
        int total = results.stream().mapToInt(UploadFileResult::imported).sum();
        return ResponseEntity.ok(new UploadResults(results, total));
    }

    public record UploadFileResult(String file, int imported) {}
    public record UploadResults(java.util.List<UploadFileResult> results, int total) {}
}
