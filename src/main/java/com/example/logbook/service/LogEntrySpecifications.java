package com.example.logbook.service;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.domain.LogLevel;
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

    public static Specification<LogEntry> level(LogLevel level) {
        return (root, query, cb) -> level == null ? null : cb.equal(root.get("logLevel"), level);
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
