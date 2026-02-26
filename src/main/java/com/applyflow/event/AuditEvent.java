package com.applyflow.event;

import com.applyflow.enums.AuditEventType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AuditEvent extends ApplicationEvent {

    private final AuditEventType eventType;
    private final Long userId;
    private final Long entityId;
    private final String metadata;

    public AuditEvent(Object source, AuditEventType eventType, Long userId, Long entityId, String metadata) {
        super(source);
        this.eventType = eventType;
        this.userId = userId;
        this.entityId = entityId;
        this.metadata = metadata;
    }

    public AuditEvent(Object source, AuditEventType eventType, Long userId) {
        this(source, eventType, userId, null, null);
    }

    public AuditEvent(Object source, AuditEventType eventType, Long userId, String metadata) {
        this(source, eventType, userId, null, metadata);
    }
}
