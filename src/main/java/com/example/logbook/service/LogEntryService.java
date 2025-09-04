package com.example.logbook.service;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.repository.LogEntryRepository;
import com.example.logbook.repository.ServerRepository;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.time.Instant;
import java.util.NoSuchElementException;

public class LogEntryService {

    private final LogEntryRepository repository;
    private final SessionFactory sessionFactory;

    public LogEntryService(LogEntryRepository repository, ServerRepository serverRepository) {
        this.repository = repository;
        this.sessionFactory = repository.getSessionFactory();
    }

    public LogEntryRepository.PageResult<LogEntry> search(Instant from,
                                 java.util.List<String> levels,
                                 String source,
                                 String query,
                                 int page, int size) {
        Transaction tx = sessionFactory.getCurrentSession().beginTransaction();
        try {
            LogEntryRepository.PageResult<LogEntry> result = repository.findWithFilters(
                from, null, levels, source, query, null, page, size, "timestamp", true);
            tx.commit();
            return result;
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    public LogEntryRepository.PageResult<LogEntry> search(Instant from,
                                 Instant to,
                                 java.util.List<String> levels,
                                 String source,
                                 String query,
                                 int page, int size) {
        Transaction tx = sessionFactory.getCurrentSession().beginTransaction();
        try {
            LogEntryRepository.PageResult<LogEntry> result = repository.findWithFilters(
                from, to, levels, source, query, null, page, size, "timestamp", true);
            tx.commit();
            return result;
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    public LogEntryRepository.PageResult<LogEntry> searchByServer(Long serverId,
                                         Instant from,
                                         Instant to,
                                         java.util.List<String> levels,
                                         String source,
                                         String query,
                                         int page, int size) {
        Transaction tx = sessionFactory.getCurrentSession().beginTransaction();
        try {
            LogEntryRepository.PageResult<LogEntry> result = repository.findWithFilters(
                from, to, levels, source, query, serverId, page, size, "timestamp", true);
            tx.commit();
            return result;
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    public LogEntry get(long id) {
        Transaction tx = sessionFactory.getCurrentSession().beginTransaction();
        try {
            LogEntry result = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("LogEntry not found: " + id));
            tx.commit();
            return result;
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    public LogEntry create(LogEntry entry) {
        Transaction tx = sessionFactory.getCurrentSession().beginTransaction();
        try {
            if (entry.getTimestamp() == null) {
                entry.setTimestamp(Instant.now());
            }
            LogEntry result = repository.save(entry);
            tx.commit();
            return result;
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    public LogEntry update(long id, LogEntry updated) {
        Transaction tx = sessionFactory.getCurrentSession().beginTransaction();
        try {
            LogEntry existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("LogEntry not found: " + id));
            existing.setTimestamp(updated.getTimestamp() != null ? updated.getTimestamp() : existing.getTimestamp());
            existing.setLogLevel(updated.getLogLevel() != null ? updated.getLogLevel() : existing.getLogLevel());
            existing.setSource(updated.getSource() != null ? updated.getSource() : existing.getSource());
            existing.setMessage(updated.getMessage() != null ? updated.getMessage() : existing.getMessage());
            existing.setCategory(updated.getCategory());
            LogEntry result = repository.save(existing);
            tx.commit();
            return result;
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    public void delete(long id) {
        Transaction tx = sessionFactory.getCurrentSession().beginTransaction();
        try {
            repository.deleteById(id);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }
}
