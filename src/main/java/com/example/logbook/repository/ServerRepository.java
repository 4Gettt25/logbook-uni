package com.example.logbook.repository;

import com.example.logbook.domain.Server;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class ServerRepository {
    
    private final SessionFactory sessionFactory;
    
    public ServerRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    public Server save(Server server) {
        return sessionFactory.getCurrentSession().merge(server);
    }
    
    public Server saveAndFlush(Server server) {
        Server saved = sessionFactory.getCurrentSession().merge(server);
        sessionFactory.getCurrentSession().flush();
        return saved;
    }
    
    public Optional<Server> findById(Long id) {
        Server server = sessionFactory.getCurrentSession().get(Server.class, id);
        return Optional.ofNullable(server);
    }
    
    public Optional<Server> findByName(String name) {
        List<Server> servers = sessionFactory.getCurrentSession()
                .createQuery("FROM Server s WHERE s.name = :name", Server.class)
                .setParameter("name", name)
                .getResultList();
        return servers.isEmpty() ? Optional.empty() : Optional.of(servers.get(0));
    }
    
    public boolean existsById(Long id) {
        Long count = sessionFactory.getCurrentSession()
                .createQuery("SELECT COUNT(s) FROM Server s WHERE s.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
        return count > 0;
    }
    
    public void deleteById(Long id) {
        Server server = sessionFactory.getCurrentSession().get(Server.class, id);
        if (server != null) {
            sessionFactory.getCurrentSession().remove(server);
        }
    }
    
    public LogEntryRepository.PageResult<Server> findAll(int page, int size, String sortBy, boolean desc) {
        EntityManager em = sessionFactory.getCurrentSession();
        
        // Count query
        Long totalCount = em.createQuery("SELECT COUNT(s) FROM Server s", Long.class)
                .getSingleResult();
        
        // Data query
        String orderClause = desc ? " ORDER BY s." + sortBy + " DESC" : " ORDER BY s." + sortBy + " ASC";
        TypedQuery<Server> query = em.createQuery("FROM Server s" + orderClause, Server.class);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        
        List<Server> content = query.getResultList();
        
        return new LogEntryRepository.PageResult<>(content, page, size, totalCount);
    }
}

