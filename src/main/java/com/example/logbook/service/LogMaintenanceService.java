package com.example.logbook.service;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.repository.LogEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class LogMaintenanceService {

    private final LogEntryRepository repository;

    public LogMaintenanceService(LogEntryRepository repository) {
        this.repository = repository;
    }

    public record ReevalResult(long scanned, long updatedLevels, long merged, long deleted, long unchanged) {}

    @Transactional
    public ReevalResult reevaluateServer(Long serverId, boolean mergeContinuations, boolean dryRun) {
        List<LogEntry> items = repository.findByServer_IdOrderByTimestampAscIdAsc(serverId);
        long scanned = 0, updated = 0, merged = 0, deleted = 0, unchanged = 0;
        List<LogEntry> toSave = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();

        LogEntry prev = null;
        for (LogEntry e : items) {
            scanned++;
            String originalMessage = nvl(e.getMessage());
            String newLevel = detectLevel(originalMessage);
            boolean levelChanged = newLevel != null && !newLevel.equals(e.getLogLevel());

            if (mergeContinuations && prev != null && looksLikeContinuation(prev, e)) {
                // Append current message to previous
                String combined = nvl(prev.getMessage());
                if (!combined.isEmpty()) combined += "\n";
                combined += originalMessage;
                // no truncation; DB column is TEXT
                prev.setMessage(combined);
                // Recompute level for prev (may still be LOG)
                String recomputed = detectLevel(combined);
                if (recomputed != null && !recomputed.equals(prev.getLogLevel())) {
                    prev.setLogLevel(recomputed);
                }
                if (!dryRun) {
                    toSave.add(prev);
                    toDelete.add(e.getId());
                }
                merged++; deleted++;
                continue;
            }

            if (levelChanged) {
                if (!dryRun) {
                    e.setLogLevel(newLevel);
                    toSave.add(e);
                }
                updated++;
            } else {
                unchanged++;
            }
            prev = e;
        }

        if (!dryRun) {
            if (!toSave.isEmpty()) repository.saveAll(toSave);
            if (!toDelete.isEmpty()) repository.deleteAllByIdInBatch(toDelete);
        }

        return new ReevalResult(scanned, updated, merged, deleted, unchanged);
    }

    private static final Pattern PG_TOKEN = Pattern.compile(
            "\\b(ERROR|FATAL|PANIC|WARNING|WARN|NOTICE|INFO|LOG|DEBUG[0-5]?|STATEMENT|DETAIL|HINT|CONTEXT)\\s*:",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NGINX_REQ = Pattern.compile(
            "\"(?:GET|POST|PUT|DELETE|HEAD|PATCH|OPTIONS)\\s+[^\"\\s]+\\s+HTTP/\\d\\.\\d\"\\s+(\\d{3})\\b",
            Pattern.CASE_INSENSITIVE);

    private String detectLevel(String message) {
        if (message == null) return null;
        var pg = PG_TOKEN.matcher(message);
        if (pg.find()) {
            String tok = pg.group(1).toUpperCase();
            if (tok.startsWith("DEBUG")) tok = "DEBUG";
            if (tok.equals("WARNING")) tok = "WARN";
            if (tok.equals("PANIC")) tok = "FATAL";
            if (tok.equals("STATEMENT") || tok.equals("DETAIL") || tok.equals("HINT") || tok.equals("CONTEXT") || tok.equals("NOTICE")) {
                tok = "LOG";
            }
            return tok;
        }
        var ng = NGINX_REQ.matcher(message);
        if (ng.find()) return ng.group(1);
        return null;
    }

    private boolean looksLikeContinuation(LogEntry prev, LogEntry curr) {
        String pm = nvl(prev.getMessage());
        String cm = nvl(curr.getMessage());
        // Do not merge if current starts with severity token
        if (PG_TOKEN.matcher(cm).find()) return false;
        // Consider merge when previous contains statement-like context and current looks like SQL or ends with ';'
        boolean prevHasContext = pm.contains("STATEMENT:") || pm.contains("DETAIL:") || pm.contains("HINT:") || pm.contains("CONTEXT:") || pm.contains("ERROR:");
        boolean currLooksSQL = cm.matches("(?is)^\\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|WITH|BEGIN|COMMIT|ROLLBACK|EXPLAIN|ANALYZE)\\b.*") || cm.trim().endsWith(";");
        return prevHasContext && currLooksSQL;
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
