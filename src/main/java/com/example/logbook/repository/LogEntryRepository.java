package com.example.logbook.repository;

import com.example.logbook.domain.LogEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.hibernate.SessionFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LogEntryRepository {
    
    private final SessionFactory sessionFactory;
    
    public LogEntryRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
    
    public LogEntry save(LogEntry logEntry) {
        return sessionFactory.getCurrentSession().merge(logEntry);
    }
    
    public Optional<LogEntry> findById(Long id) {
        LogEntry logEntry = sessionFactory.getCurrentSession().get(LogEntry.class, id);
        return Optional.ofNullable(logEntry);
    }
    
    public void deleteById(Long id) {
        LogEntry logEntry = sessionFactory.getCurrentSession().get(LogEntry.class, id);
        if (logEntry != null) {
            sessionFactory.getCurrentSession().remove(logEntry);
        }
    }
    
    public void deleteByServerId(Long serverId) {
        sessionFactory.getCurrentSession()
                .createQuery("DELETE FROM LogEntry e WHERE e.server.id = :serverId")
                .setParameter("serverId", serverId)
                .executeUpdate();
    }
    
    public long countByServerId(Long serverId) {
        return sessionFactory.getCurrentSession()
                .createQuery("SELECT COUNT(e) FROM LogEntry e WHERE e.server.id = :serverId", Long.class)
                .setParameter("serverId", serverId)
                .getSingleResult();
    }
    
    public List<String> findDistinctLevels() {
        return sessionFactory.getCurrentSession()
                .createQuery("SELECT DISTINCT e.logLevel FROM LogEntry e", String.class)
                .getResultList();
    }
    
    public List<String> findDistinctLevelsByServerId(Long serverId) {
        return sessionFactory.getCurrentSession()
                .createQuery("SELECT DISTINCT e.logLevel FROM LogEntry e WHERE e.server.id = :serverId", String.class)
                .setParameter("serverId", serverId)
                .getResultList();
    }
    
    public List<LogEntry> findByServerIdOrderByTimestampAscIdAsc(Long serverId) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM LogEntry e WHERE e.server.id = :serverId ORDER BY e.timestamp ASC, e.id ASC", LogEntry.class)
                .setParameter("serverId", serverId)
                .getResultList();
    }
    
    public PageResult<LogEntry> findWithFilters(Instant from, Instant to, List<String> levels, 
                                               String source, String query, Long serverId, 
                                               int page, int size, String sortBy, boolean desc) {
        EntityManager em = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        
        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<LogEntry> countRoot = countQuery.from(LogEntry.class);
        countQuery.select(cb.count(countRoot));
        applyFilters(cb, countQuery, countRoot, from, to, levels, source, query, serverId);
        Long totalCount = em.createQuery(countQuery).getSingleResult();
        
        // Data query
        CriteriaQuery<LogEntry> dataQuery = cb.createQuery(LogEntry.class);
        Root<LogEntry> dataRoot = dataQuery.from(LogEntry.class);
        dataQuery.select(dataRoot);
        applyFilters(cb, dataQuery, dataRoot, from, to, levels, source, query, serverId);
        
        // Apply sorting
        Order order = desc ? cb.desc(dataRoot.get(sortBy)) : cb.asc(dataRoot.get(sortBy));
        dataQuery.orderBy(order);
        
        TypedQuery<LogEntry> typedQuery = em.createQuery(dataQuery);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        
        List<LogEntry> content = typedQuery.getResultList();
        
        return new PageResult<>(content, page, size, totalCount);
    }
    
    private void applyFilters(CriteriaBuilder cb, CriteriaQuery<?> query, Root<LogEntry> root,
                             Instant from, Instant to, List<String> levels, 
                             String source, String queryText, Long serverId) {
        List<Predicate> predicates = new ArrayList<>();
        
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        }
        
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
        }
        
        if (levels != null && !levels.isEmpty()) {
            predicates.add(root.get("logLevel").in(levels));
        }
        
        if (source != null && !source.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("source")), "%" + source.toLowerCase() + "%"));
        }
        
        if (queryText != null && !queryText.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("message")), "%" + queryText.toLowerCase() + "%"));
        }
        
        if (serverId != null) {
            predicates.add(cb.equal(root.get("server").get("id"), serverId));
        }
        
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }
    }
    
    public static class PageResult<T> {
        private final List<T> content;
        private final int pageNumber;
        private final int pageSize;
        private final long totalElements;
        private final int totalPages;
        
        public PageResult(List<T> content, int pageNumber, int pageSize, long totalElements) {
            this.content = content;
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
        }
        
        // Getters
        public List<T> getContent() { return content; }
        public int getNumber() { return pageNumber; }
        public int getSize() { return pageSize; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
        public boolean hasNext() { return pageNumber < totalPages - 1; }
        public boolean hasPrevious() { return pageNumber > 0; }
        public boolean isFirst() { return pageNumber == 0; }
        public boolean isLast() { return pageNumber >= totalPages - 1; }
    }
}
