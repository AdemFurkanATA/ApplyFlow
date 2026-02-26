package com.applyflow.service;

import com.applyflow.dto.AuditLogResponse;
import com.applyflow.dto.PagedResponse;
import com.applyflow.enums.AuditEventType;
import com.applyflow.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAuditLogs(
            Long userId,
            AuditEventType eventType,
            Instant startDate,
            Instant endDate,
            Pageable pageable) {

        var page = auditLogRepository.findByFilters(userId, eventType, startDate, endDate, pageable);

        var content = page.getContent().stream()
                .map(log -> AuditLogResponse.builder()
                        .id(log.getId())
                        .eventType(log.getEventType())
                        .userId(log.getUserId())
                        .entityId(log.getEntityId())
                        .metadata(log.getMetadata())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();

        return PagedResponse.<AuditLogResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
