package com.example.logbook.service;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.repository.LogEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;

@Service
@Transactional
public class LogEntryService {

    private final LogEntryRepository repository;

    public LogEntryService(LogEntryRepository repository) {
        this.repository = repository;
    }

    public Page<LogEntry> search(Instant from,
                                 Instant to,
                                 java.util.List<String> levels,
                                 String source,
                                 String query,
                                 Pageable pageable) {
        Specification<LogEntry> spec = Specification
                .where(LogEntrySpecifications.timestampFrom(from))
                .and(LogEntrySpecifications.timestampTo(to))
                .and(LogEntrySpecifications.levels(levels))
                .and(LogEntrySpecifications.source(source))
                .and(LogEntrySpecifications.message(query));
        return repository.findAll(spec, pageable);
    }

    public Page<LogEntry> searchByServer(Long serverId,
                                         Instant from,
                                         Instant to,
                                         java.util.List<String> levels,
                                         String source,
                                         String query,
                                         Pageable pageable) {
        Specification<LogEntry> spec = Specification
                .where(LogEntrySpecifications.serverId(serverId))
                .and(LogEntrySpecifications.timestampFrom(from))
                .and(LogEntrySpecifications.timestampTo(to))
                .and(LogEntrySpecifications.levels(levels))
                .and(LogEntrySpecifications.source(source))
                .and(LogEntrySpecifications.message(query));
        return repository.findAll(spec, pageable);
    }

    public LogEntry get(long id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("LogEntry not found: " + id));
    }

    public LogEntry create(LogEntry entry) {
        if (entry.getTimestamp() == null) {
            entry.setTimestamp(Instant.now());
        }
        return repository.save(entry);
    }

    public LogEntry update(long id, LogEntry updated) {
        LogEntry existing = get(id);
        existing.setTimestamp(updated.getTimestamp() != null ? updated.getTimestamp() : existing.getTimestamp());
        existing.setLogLevel(updated.getLogLevel() != null ? updated.getLogLevel() : existing.getLogLevel());
        existing.setSource(updated.getSource() != null ? updated.getSource() : existing.getSource());
        existing.setMessage(updated.getMessage() != null ? updated.getMessage() : existing.getMessage());
        existing.setCategory(updated.getCategory());
        return repository.save(existing);
    }

    public void delete(long id) {
        repository.deleteById(id);
    }
}
