package com.example.logbook.repository;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.domain.LogLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long>, JpaSpecificationExecutor<LogEntry> {
    long countByServer_Id(Long serverId);
    void deleteByServer_Id(Long serverId);

    @Query("select distinct e.logLevel from LogEntry e")
    List<LogLevel> findDistinctLevels();

    @Query("select distinct e.logLevel from LogEntry e where e.server.id = :serverId")
    List<LogLevel> findDistinctLevelsByServerId(@Param("serverId") Long serverId);
}
