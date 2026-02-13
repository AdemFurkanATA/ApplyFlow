package com.applyflow.dto;

import com.applyflow.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatusHistoryResponse {

    private Long id;
    private ApplicationStatus previousStatus;
    private ApplicationStatus newStatus;
    private LocalDateTime changedAt;
}
