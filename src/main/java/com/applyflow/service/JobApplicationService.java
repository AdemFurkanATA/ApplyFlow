package com.applyflow.service;

import com.applyflow.dto.*;
import com.applyflow.entity.JobApplication;
import com.applyflow.entity.StatusHistory;
import com.applyflow.entity.User;
import com.applyflow.enums.ApplicationStatus;
import com.applyflow.exception.ResourceNotFoundException;
import com.applyflow.exception.UnauthorizedAccessException;
import com.applyflow.mapper.JobApplicationMapper;
import com.applyflow.repository.JobApplicationRepository;
import com.applyflow.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final JobApplicationMapper mapper;
    private final EmailService emailService;

    @Transactional
    public JobApplicationResponse create(JobApplicationRequest request, User user) {
        JobApplication application = mapper.toEntity(request);
        application.setUser(user);
        application = applicationRepository.save(application);
        log.debug("Created job application {} for user {}", application.getId(), user.getId());
        return mapper.toResponse(application);
    }

    @Transactional(readOnly = true)
    public PagedResponse<JobApplicationResponse> getAll(
            User user,
            ApplicationStatus status,
            String companyName,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        Page<JobApplication> page = applicationRepository.findByFilters(
                user, status, companyName, startDate, endDate, pageable);

        List<JobApplicationResponse> content = page.getContent()
                .stream()
                .map(mapper::toResponse)
                .toList();

        return PagedResponse.<JobApplicationResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public JobApplicationResponse getById(Long id, User user) {
        JobApplication application = findApplicationByIdAndUser(id, user);
        return mapper.toResponse(application);
    }

    @Transactional
    public JobApplicationResponse update(Long id, JobApplicationRequest request, User user) {
        JobApplication application = findApplicationByIdAndUser(id, user);

        ApplicationStatus oldStatus = application.getStatus();
        mapper.updateEntity(application, request);

        if (request.getStatus() != null && oldStatus != request.getStatus()) {
            StatusHistory history = StatusHistory.builder()
                    .jobApplication(application)
                    .previousStatus(oldStatus)
                    .newStatus(request.getStatus())
                    .build();
            statusHistoryRepository.save(history);
            log.debug("Status changed from {} to {} for application {}",
                    oldStatus, request.getStatus(), id);
            emailService.sendStatusChangeNotification(
                    user.getEmail(), application.getCompanyName(),
                    oldStatus.name(), request.getStatus().name());
        }

        application = applicationRepository.save(application);
        return mapper.toResponse(application);
    }

    @Transactional
    public void delete(Long id, User user) {
        JobApplication application = findApplicationByIdAndUser(id, user);
        applicationRepository.delete(application);
        log.debug("Deleted job application {} for user {}", id, user.getId());
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getStatusHistory(Long applicationId, User user) {
        JobApplication application = findApplicationByIdAndUser(applicationId, user);

        return statusHistoryRepository
                .findByJobApplicationOrderByChangedAtDesc(application)
                .stream()
                .map(mapper::toStatusHistoryResponse)
                .toList();
    }

    private JobApplication findApplicationByIdAndUser(Long id, User user) {
        return applicationRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job application not found with id: " + id));
    }
}
