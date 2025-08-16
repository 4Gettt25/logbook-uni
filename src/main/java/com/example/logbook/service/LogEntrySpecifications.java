package com.example.logbook.service;

import com.example.logbook.domain.LogEntry;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class LogEntrySpecifications {
    private LogEntrySpecifications() {}

    public static Specification<LogEntry> timestampFrom(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("timestamp"), from);
    }

    public static Specification<LogEntry> timestampTo(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("timestamp"), to);
    }

    public static Specification<LogEntry> levels(java.util.List<String> levels) {
        return (root, query, cb) -> {
            if (levels == null || levels.isEmpty()) return null;
            var preds = levels.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> cb.equal(root.get("logLevel"), s))
                    .toArray(jakarta.persistence.criteria.Predicate[]::new);
            if (preds.length == 0) return null;
            return cb.or(preds);
        };
    }

    public static Specification<LogEntry> source(String source) {
        return (root, query, cb) -> (source == null || source.isBlank()) ? null : cb.like(cb.lower(root.get("source")), like(source));
    }

    public static Specification<LogEntry> message(String q) {
        return (root, query, cb) -> (q == null || q.isBlank()) ? null : cb.like(cb.lower(root.get("message")), like(q));
    }

    // status and username have been removed

    public static Specification<LogEntry> serverId(Long serverId) {
        return (root, query, cb) -> serverId == null ? null : cb.equal(root.get("server").get("id"), serverId);
    }

    private static String like(String value) {
        return "%" + value.toLowerCase().trim() + "%";
    }
}
