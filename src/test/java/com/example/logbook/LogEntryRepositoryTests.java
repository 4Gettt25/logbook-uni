package com.example.logbook;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.domain.LogLevel;
import com.example.logbook.domain.LogStatus;
import com.example.logbook.repository.LogEntryRepository;
import com.example.logbook.service.LogEntrySpecifications;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LogEntryRepositoryTests {

    @Autowired
    LogEntryRepository repository;

    @Test
    void savesAndFiltersByLevelAndSource() {
        LogEntry e1 = new LogEntry();
        e1.setTimestamp(Instant.now());
        e1.setLogLevel(LogLevel.INFO);
        e1.setSource("auth");
        e1.setMessage("User logged in");
        e1.setStatus(LogStatus.OPEN);
        repository.save(e1);

        LogEntry e2 = new LogEntry();
        e2.setTimestamp(Instant.now());
        e2.setLogLevel(LogLevel.ERROR);
        e2.setSource("api");
        e2.setMessage("Unhandled exception");
        e2.setStatus(LogStatus.OPEN);
        repository.save(e2);

        Specification<LogEntry> spec = Specification
                .where(LogEntrySpecifications.level(LogLevel.ERROR))
                .and(LogEntrySpecifications.source("api"));
        List<LogEntry> results = repository.findAll(spec);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMessage()).contains("Unhandled");
    }
}

