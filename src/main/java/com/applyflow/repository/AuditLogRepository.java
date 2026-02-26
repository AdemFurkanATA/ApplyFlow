package com.applyflow.repository;

import com.applyflow.entity.AuditLog;
import com.applyflow.enums.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByEventType(AuditEventType eventType, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:eventType IS NULL OR a.eventType = :eventType) AND " +
            "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR a.createdAt <= :endDate)")
    Page<AuditLog> findByFilters(Long userId, AuditEventType eventType,
            Instant startDate, Instant endDate,
            Pageable pageable);
}
