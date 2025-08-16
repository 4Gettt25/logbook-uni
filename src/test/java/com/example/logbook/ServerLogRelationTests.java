package com.example.logbook;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.domain.LogLevel;
import com.example.logbook.domain.Server;
import com.example.logbook.repository.LogEntryRepository;
import com.example.logbook.repository.ServerRepository;
import com.example.logbook.service.LogEntrySpecifications;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ServerLogRelationTests {

    @Autowired
    ServerRepository servers;
    @Autowired
    LogEntryRepository logs;

    @Test
    void associatesLogsToServerAndFilters() {
        Server s1 = new Server();
        s1.setName("server-a");
        servers.save(s1);

        Server s2 = new Server();
        s2.setName("server-b");
        servers.save(s2);

        LogEntry a = new LogEntry();
        a.setTimestamp(Instant.now());
        a.setLogLevel(LogLevel.INFO);
        a.setSource("app");
        a.setMessage("hello A");
        a.setServer(s1);
        logs.save(a);

        LogEntry b = new LogEntry();
        b.setTimestamp(Instant.now());
        b.setLogLevel(LogLevel.INFO);
        b.setSource("app");
        b.setMessage("hello B");
        b.setServer(s2);
        logs.save(b);

        List<LogEntry> onlyA = logs.findAll(Specification.where(LogEntrySpecifications.serverId(s1.getId())));
        assertThat(onlyA).extracting(LogEntry::getMessage).containsExactly("hello A");
    }
}

