package com.example.logbook.service;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.domain.LogLevel;
import com.example.logbook.domain.LogStatus;
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
        LogLevel level = LogLevel.INFO;
        String source = "upload";
        String msg = line;

        Matcher m = ISO_LINE.matcher(line);
        if (m.matches()) {
            String tsStr = m.group("ts");
            try { ts = Instant.parse(tsStr.endsWith("Z") ? tsStr : tsStr + "Z"); } catch (DateTimeParseException ignored) {}
            try { level = LogLevel.valueOf(m.group("level")); } catch (IllegalArgumentException ignored) {}
            source = m.group("source");
            msg = m.group("msg");
        } else {
            m = LOG4J_LINE.matcher(line);
            if (m.matches()) {
                String tsStr = m.group("ts");
                try {
                    ts = parseLog4jTs(tsStr);
                } catch (Exception ignored) {}
                try { level = LogLevel.valueOf(m.group("level")); } catch (IllegalArgumentException ignored) {}
                source = m.group("source");
                msg = m.group("msg");
            } else {
                m = SYSLOG_LINE.matcher(line);
                if (m.matches()) {
                    try { ts = parseSyslogTs(m.group("mon"), m.group("day"), m.group("time")); } catch (Exception ignored) {}
                    source = m.group("source");
                    msg = m.group("msg");
                    level = LogLevel.INFO;
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
        e.setStatus(LogStatus.OPEN);
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
