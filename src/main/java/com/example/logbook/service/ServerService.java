package com.example.logbook.service;

import com.example.logbook.domain.Server;
import com.example.logbook.repository.LogEntryRepository;
import com.example.logbook.repository.ServerRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

public class ServerService {

    private final ServerRepository servers;
    private final LogEntryRepository logEntries;
    private final SessionFactory sessionFactory;

    public ServerService(ServerRepository servers, LogEntryRepository logEntries, SessionFactory sessionFactory) {
        this.servers = servers;
        this.logEntries = logEntries;
        this.sessionFactory = sessionFactory;
    }

    public LogEntryRepository.PageResult<Server> list(int page, int size, String sortBy, boolean desc) {
        return execute(session -> servers.findAll(page, size, sortBy, desc));
    }

    public Server get(long id) {
        return execute(session -> servers.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Server not found: " + id)));
    }

    public Server create(Server server) {
        return execute(session -> servers.saveAndFlush(server));
    }

    public void delete(long id) {
        executeVoid(session -> {
            if (!servers.existsById(id)) {
                throw new NoSuchElementException("Server not found: " + id);
            }
            logEntries.deleteByServerId(id);
            servers.deleteById(id);
        });
    }

    public List<String> listLogLevels(long serverId) {
        return execute(session -> {
            servers.findById(serverId)
                .orElseThrow(() -> new NoSuchElementException("Server not found: " + serverId));
            List<String> levels = logEntries.findDistinctLevelsByServerId(serverId);
            levels.sort(String.CASE_INSENSITIVE_ORDER);
            return levels;
        });
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
