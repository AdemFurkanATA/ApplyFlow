package com.applyflow.mapper;

import com.applyflow.dto.JobApplicationRequest;
import com.applyflow.dto.JobApplicationResponse;
import com.applyflow.dto.StatusHistoryResponse;
import com.applyflow.entity.JobApplication;
import com.applyflow.entity.StatusHistory;
import com.applyflow.enums.ApplicationStatus;
import org.springframework.stereotype.Component;

@Component
public class JobApplicationMapper {

    public JobApplication toEntity(JobApplicationRequest request) {
        return JobApplication.builder()
                .companyName(request.getCompanyName())
                .position(request.getPosition())
                .status(request.getStatus() != null ? request.getStatus() : ApplicationStatus.APPLIED)
                .applicationDate(request.getApplicationDate())
                .salaryExpectation(request.getSalaryExpectation())
                .contactPerson(request.getContactPerson())
                .notes(request.getNotes())
                .build();
    }

    public JobApplicationResponse toResponse(JobApplication entity) {
        return JobApplicationResponse.builder()
                .id(entity.getId())
                .companyName(entity.getCompanyName())
                .position(entity.getPosition())
                .status(entity.getStatus())
                .applicationDate(entity.getApplicationDate())
                .salaryExpectation(entity.getSalaryExpectation())
                .contactPerson(entity.getContactPerson())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(JobApplication entity, JobApplicationRequest request) {
        entity.setCompanyName(request.getCompanyName());
        entity.setPosition(request.getPosition());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        entity.setApplicationDate(request.getApplicationDate());
        entity.setSalaryExpectation(request.getSalaryExpectation());
        entity.setContactPerson(request.getContactPerson());
        entity.setNotes(request.getNotes());
    }

    public StatusHistoryResponse toStatusHistoryResponse(StatusHistory history) {
        return StatusHistoryResponse.builder()
                .id(history.getId())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .changedAt(history.getChangedAt())
                .build();
    }
}
