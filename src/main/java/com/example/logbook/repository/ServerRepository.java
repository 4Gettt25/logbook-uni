package com.example.logbook.repository;

import com.example.logbook.domain.Server;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServerRepository extends JpaRepository<Server, Long> {
    Optional<Server> findByName(String name);
}

