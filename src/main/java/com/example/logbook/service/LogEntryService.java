package com.example.logbook.service;

import com.example.logbook.domain.LogEntry;
import com.example.logbook.repository.LogEntryRepository;
import com.example.logbook.repository.ServerRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

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
        return execute(session -> repository.findWithFilters(
            from, null, levels, source, query, null, page, size, "timestamp", true));
    }

    public LogEntryRepository.PageResult<LogEntry> search(Instant from,
                                 Instant to,
                                 java.util.List<String> levels,
                                 String source,
                                 String query,
                                 int page, int size) {
        return execute(session -> repository.findWithFilters(
            from, to, levels, source, query, null, page, size, "timestamp", true));
    }

    public LogEntryRepository.PageResult<LogEntry> searchByServer(Long serverId,
                                         Instant from,
                                         Instant to,
                                         java.util.List<String> levels,
                                         String source,
                                         String query,
                                         int page, int size) {
        return execute(session -> repository.findWithFilters(
            from, to, levels, source, query, serverId, page, size, "timestamp", true));
    }

    public LogEntry get(long id) {
        return execute(session -> repository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("LogEntry not found: " + id)));
    }

    public LogEntry create(LogEntry entry) {
        return execute(session -> {
            if (entry.getTimestamp() == null) {
                entry.setTimestamp(Instant.now());
            }
            return repository.save(entry);
        });
    }

    public LogEntry update(long id, LogEntry updated) {
        return execute(session -> {
            LogEntry existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("LogEntry not found: " + id));
            existing.setTimestamp(updated.getTimestamp() != null ? updated.getTimestamp() : existing.getTimestamp());
            existing.setLogLevel(updated.getLogLevel() != null ? updated.getLogLevel() : existing.getLogLevel());
            existing.setSource(updated.getSource() != null ? updated.getSource() : existing.getSource());
            existing.setMessage(updated.getMessage() != null ? updated.getMessage() : existing.getMessage());
            existing.setCategory(updated.getCategory());
            return repository.save(existing);
        });
    }

    public void delete(long id) {
        executeVoid(session -> repository.deleteById(id));
    }

    private <T> T execute(Function<Session, T> work) {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);
        Transaction tx = session.beginTransaction();
        try {
            T result = work.apply(session);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            tx.rollback();
            throw e;
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }

    private void executeVoid(Consumer<Session> work) {
        execute(session -> {
            work.accept(session);
            return null;
        });
    }
}
