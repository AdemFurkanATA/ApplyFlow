package com.applyflow.event;

import com.applyflow.entity.AuditLog;
import com.applyflow.enums.AuditEventType;
import com.applyflow.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditEventListener auditEventListener;

    @Test
    @DisplayName("Should save audit log for user registered event")
    void handleAuditEvent_UserRegistered() {
        AuditEvent event = new AuditEvent(this, AuditEventType.USER_REGISTERED, 1L);

        auditEventListener.handleAuditEvent(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(AuditEventType.USER_REGISTERED);
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getEntityId()).isNull();
    }

    @Test
    @DisplayName("Should save audit log with entity ID and metadata")
    void handleAuditEvent_WithEntityAndMetadata() {
        AuditEvent event = new AuditEvent(this, AuditEventType.JOB_STATUS_CHANGED,
                1L, 42L, "APPLIED -> INTERVIEW");

        auditEventListener.handleAuditEvent(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(AuditEventType.JOB_STATUS_CHANGED);
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getEntityId()).isEqualTo(42L);
        assertThat(saved.getMetadata()).isEqualTo("APPLIED -> INTERVIEW");
    }

    @Test
    @DisplayName("Should not throw when repository fails (fail-safe)")
    void handleAuditEvent_RepositoryFailure() {
        AuditEvent event = new AuditEvent(this, AuditEventType.USER_LOGGED_IN, 1L);
        doThrow(new RuntimeException("DB down")).when(auditLogRepository).save(any());

        // Should not throw — fail-safe behavior
        auditEventListener.handleAuditEvent(event);

        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Should save all event types correctly")
    void handleAuditEvent_AllEventTypes() {
        for (AuditEventType type : AuditEventType.values()) {
            AuditEvent event = new AuditEvent(this, type, 1L);
            auditEventListener.handleAuditEvent(event);
        }

        verify(auditLogRepository, times(AuditEventType.values().length)).save(any());
    }
}
