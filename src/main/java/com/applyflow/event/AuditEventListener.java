package com.applyflow.event;

import com.applyflow.entity.AuditLog;
import com.applyflow.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAuditEvent(AuditEvent event) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .eventType(event.getEventType())
                    .userId(event.getUserId())
                    .entityId(event.getEntityId())
                    .metadata(event.getMetadata())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: type={}, userId={}", event.getEventType(), event.getUserId());
        } catch (Exception e) {
            log.error("Failed to save audit log for event {}: {}", event.getEventType(), e.getMessage());
        }
    }
}
