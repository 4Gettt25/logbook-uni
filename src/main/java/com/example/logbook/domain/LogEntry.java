package com.example.logbook.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "log_entry", indexes = {
        @Index(name = "idx_log_entry_timestamp", columnList = "timestamp"),
        @Index(name = "idx_log_entry_loglevel", columnList = "log_level"),
        @Index(name = "idx_log_entry_source", columnList = "source"),
        @Index(name = "idx_log_entry_server", columnList = "server_id")
})
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", nullable = false)
    private LogLevel logLevel;

    @NotBlank
    @Size(max = 255)
    private String source;

    @NotBlank
    @Size(max = 4000)
    private String message;

    @Size(max = 255)
    private String username;

    @Size(max = 255)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogStatus status = LogStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    @JsonIgnore
    private Server server;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LogStatus getStatus() {
        return status;
    }

    public void setStatus(LogStatus status) {
        this.status = status;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }
}
