package com.example.logbook.repository;

import com.example.logbook.domain.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long>, JpaSpecificationExecutor<LogEntry> {
}

