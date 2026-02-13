package com.applyflow.controller;

import com.applyflow.dto.*;
import com.applyflow.entity.User;
import com.applyflow.enums.ApplicationStatus;
import com.applyflow.service.JobApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Job Applications", description = "CRUD operations for job applications")
public class JobApplicationController {

    private final JobApplicationService applicationService;

    @PostMapping
    @Operation(summary = "Create a new job application")
    public ResponseEntity<JobApplicationResponse> create(
            @Valid @RequestBody JobApplicationRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.create(request, user));
    }

    @GetMapping
    @Operation(summary = "List all applications with filtering and pagination")
    public ResponseEntity<PagedResponse<JobApplicationResponse>> getAll(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "applicationDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(
                applicationService.getAll(user, status, companyName, startDate, endDate, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a job application by ID")
    public ResponseEntity<JobApplicationResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(applicationService.getById(id, user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a job application")
    public ResponseEntity<JobApplicationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody JobApplicationRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(applicationService.update(id, request, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a job application")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        applicationService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get status change history for an application")
    public ResponseEntity<List<StatusHistoryResponse>> getStatusHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(applicationService.getStatusHistory(id, user));
    }
}
