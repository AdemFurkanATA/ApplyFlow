package com.applyflow.event;

import com.applyflow.enums.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(AuditEventType eventType, Long userId) {
        publish(eventType, userId, null, null);
    }

    public void publish(AuditEventType eventType, Long userId, Long entityId) {
        publish(eventType, userId, entityId, null);
    }

    public void publish(AuditEventType eventType, Long userId, Long entityId, String metadata) {
        log.debug("Publishing audit event: type={}, userId={}, entityId={}", eventType, userId, entityId);
        eventPublisher.publishEvent(new AuditEvent(this, eventType, userId, entityId, metadata));
    }
}
