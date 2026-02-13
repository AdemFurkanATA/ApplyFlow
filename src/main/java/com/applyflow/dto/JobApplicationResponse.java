package com.applyflow.dto;

import com.applyflow.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobApplicationResponse {

    private Long id;
    private String companyName;
    private String position;
    private ApplicationStatus status;
    private LocalDate applicationDate;
    private BigDecimal salaryExpectation;
    private String contactPerson;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
