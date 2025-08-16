package com.example.logbook.service;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.domain.Server;
import com.example.logbook.repository.LogEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogImportService {
    private static final int MAX_MESSAGE = 4000;
    private static final Pattern ISO_LINE = Pattern.compile(
            "^(?<ts>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z?)\\s+(?<level>[A-Z]+)\\s+(?<source>[\\w.\\-]+)\\s*-?\\s*(?<msg>.*)$"
    );
    private static final Pattern LOG4J_LINE = Pattern.compile(
            "^(?<ts>\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}(?:,\\d{3})?)\\s+(?<level>[A-Z]+)\\s+(?<source>[\\w.\\-]+).*?-\\s*(?<msg>.*)$"
    );
    private static final Pattern SYSLOG_LINE = Pattern.compile(
            "^(?<mon>Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(?<day>\\d{1,2})\\s+(?<time>\\d{2}:\\d{2}:\\d{2})\\s+(?<host>\\S+)\\s+(?<source>[\\w.\\-]+)(?:\\[\\d+\\])?:\\s*(?<msg>.*)$"
    );

    private final LogEntryRepository repository;

    public LogImportService(LogEntryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int importText(byte[] bytes, Server server) {
        String content = new String(bytes, StandardCharsets.UTF_8);
        String[] lines = content.split("\r?\n");
        List<LogEntry> batch = new ArrayList<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty()) continue;
            batch.add(parseLine(line, server));
        }
        if (!batch.isEmpty()) {
            repository.saveAll(batch);
        }
        return batch.size();
    }

    private LogEntry parseLine(String line, Server server) {
        Instant ts = Instant.now();
        String level = "INFO";
        String source = "upload";
        String msg = line;

        Matcher m = ISO_LINE.matcher(line);
        if (m.matches()) {
            String tsStr = m.group("ts");
            try { ts = Instant.parse(tsStr.endsWith("Z") ? tsStr : tsStr + "Z"); } catch (DateTimeParseException ignored) {}
            String lv = m.group("level");
            if (lv != null && !lv.isBlank()) level = lv.toUpperCase();
            source = m.group("source");
            msg = m.group("msg");
        } else {
            m = LOG4J_LINE.matcher(line);
            if (m.matches()) {
                String tsStr = m.group("ts");
                try {
                    ts = parseLog4jTs(tsStr);
                } catch (Exception ignored) {}
                String lv = m.group("level");
                if (lv != null && !lv.isBlank()) level = lv.toUpperCase();
                source = m.group("source");
                msg = m.group("msg");
            } else {
                m = SYSLOG_LINE.matcher(line);
                if (m.matches()) {
                    try { ts = parseSyslogTs(m.group("mon"), m.group("day"), m.group("time")); } catch (Exception ignored) {}
                    source = m.group("source");
                    msg = m.group("msg");
                    level = "INFO";
            }
        }

        // Heuristics for other formats (nginx combined, Postgres, etc.)
        if (level == null || level.isBlank() || "INFO".equals(level)) {
            // Postgres: look for tokens like ERROR, FATAL, WARNING, LOG, DEBUG, INFO
            java.util.regex.Matcher lvlTok = java.util.regex.Pattern.compile("\\b(ERROR|FATAL|WARNING|WARN|LOG|DEBUG|INFO|TRACE)\\b", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(line);
            if (lvlTok.find()) {
                String tok = lvlTok.group(1).toUpperCase();
                level = tok.equals("WARNING") ? "WARN" : tok;
            }
        }
        if (level == null || level.isBlank() || "INFO".equals(level)) {
            // nginx combined log: capture HTTP status code (3 digits) after the quoted request
            java.util.regex.Matcher mng = java.util.regex.Pattern.compile("\"\\S+\\s+\\S+\\s+\\S+\"\\s+(?<status>\\d{3})\\b").matcher(line);
            if (mng.find()) {
                level = mng.group("status");
            } else {
                // Fallback: first standalone 3-digit number (100-599)
                java.util.regex.Matcher any3 = java.util.regex.Pattern.compile("\\b([1-5]\\d{2})\\b").matcher(line);
                if (any3.find()) {
                    level = any3.group(1);
                }
            }
        }
        }
        if (msg.length() > MAX_MESSAGE) {
            msg = msg.substring(0, MAX_MESSAGE);
        }
        if (source.length() > 255) {
            source = source.substring(0, 255);
        }
        LogEntry e = new LogEntry();
        e.setTimestamp(ts);
        e.setLogLevel(level);
        e.setSource(source);
        e.setMessage(msg);
        e.setServer(server);
        return e;
    }

    private Instant parseLog4jTs(String ts) {
        // supports "yyyy-MM-dd HH:mm:ss" and "yyyy-MM-dd HH:mm:ss,SSS" (UTC)
        java.time.format.DateTimeFormatter f1 = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneOffset.UTC);
        java.time.format.DateTimeFormatter f2 = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS").withZone(java.time.ZoneOffset.UTC);
        try {
            return java.time.LocalDateTime.parse(ts, f2).toInstant(java.time.ZoneOffset.UTC);
        } catch (Exception ignore) {
            return java.time.LocalDateTime.parse(ts, f1).toInstant(java.time.ZoneOffset.UTC);
        }
    }

    private Instant parseSyslogTs(String mon, String day, String time) {
        int year = java.time.ZonedDateTime.now().getYear();
        String ts = year + " " + mon + " " + day + " " + time;
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy MMM d HH:mm:ss");
        return java.time.LocalDateTime.parse(ts, fmt).atZone(java.time.ZoneId.systemDefault()).toInstant();
    }
}
