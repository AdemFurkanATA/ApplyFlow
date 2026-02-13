package com.applyflow.dto;

import com.applyflow.enums.ApplicationStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobApplicationRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 200, message = "Company name must not exceed 200 characters")
    private String companyName;

    @NotBlank(message = "Position is required")
    @Size(max = 200, message = "Position must not exceed 200 characters")
    private String position;

    private ApplicationStatus status;

    @NotNull(message = "Application date is required")
    @PastOrPresent(message = "Application date cannot be in the future")
    private LocalDate applicationDate;

    @DecimalMin(value = "0.0", message = "Salary expectation must be positive")
    @Digits(integer = 8, fraction = 2, message = "Invalid salary format")
    private BigDecimal salaryExpectation;

    @Size(max = 200, message = "Contact person must not exceed 200 characters")
    private String contactPerson;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
