package com.applyflow.dto;

import com.applyflow.enums.AuditEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponse {

    private Long id;
    private AuditEventType eventType;
    private Long userId;
    private Long entityId;
    private String metadata;
    private Instant createdAt;
}
