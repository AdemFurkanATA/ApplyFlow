package com.applyflow.service;

import com.applyflow.dto.JobApplicationRequest;
import com.applyflow.dto.JobApplicationResponse;
import com.applyflow.dto.PagedResponse;
import com.applyflow.entity.JobApplication;
import com.applyflow.entity.StatusHistory;
import com.applyflow.entity.User;
import com.applyflow.enums.ApplicationStatus;
import com.applyflow.enums.Role;
import com.applyflow.exception.ResourceNotFoundException;
import com.applyflow.mapper.JobApplicationMapper;
import com.applyflow.repository.JobApplicationRepository;
import com.applyflow.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    @Mock
    private JobApplicationMapper mapper;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private JobApplicationService service;

    private User user;
    private JobApplication application;
    private JobApplicationRequest request;
    private JobApplicationResponse response;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .role(Role.USER)
                .build();

        application = JobApplication.builder()
                .id(1L)
                .companyName("Google")
                .position("Backend Developer")
                .status(ApplicationStatus.APPLIED)
                .applicationDate(LocalDate.now())
                .salaryExpectation(new BigDecimal("100000"))
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        request = JobApplicationRequest.builder()
                .companyName("Google")
                .position("Backend Developer")
                .status(ApplicationStatus.APPLIED)
                .applicationDate(LocalDate.now())
                .salaryExpectation(new BigDecimal("100000"))
                .build();

        response = JobApplicationResponse.builder()
                .id(1L)
                .companyName("Google")
                .position("Backend Developer")
                .status(ApplicationStatus.APPLIED)
                .applicationDate(LocalDate.now())
                .build();
    }

    @Test
    @DisplayName("Should create job application successfully")
    void create_Success() {
        when(mapper.toEntity(request)).thenReturn(application);
        when(applicationRepository.save(any())).thenReturn(application);
        when(mapper.toResponse(application)).thenReturn(response);

        JobApplicationResponse result = service.create(request, user);

        assertThat(result).isNotNull();
        assertThat(result.getCompanyName()).isEqualTo("Google");
        verify(applicationRepository).save(any());
    }

    @Test
    @DisplayName("Should return paginated applications")
    void getAll_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobApplication> page = new PageImpl<>(List.of(application), pageable, 1);

        when(applicationRepository.findByFilters(eq(user), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(page);
        when(mapper.toResponse(any())).thenReturn(response);

        PagedResponse<JobApplicationResponse> result = service.getAll(
                user, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should get application by ID for authorized user")
    void getById_Success() {
        when(applicationRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(application));
        when(mapper.toResponse(application)).thenReturn(response);

        JobApplicationResponse result = service.getById(1L, user);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent application")
    void getById_NotFound() {
        when(applicationRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should update application and track status change")
    void update_WithStatusChange() {
        JobApplicationRequest updateRequest = JobApplicationRequest.builder()
                .companyName("Google")
                .position("Backend Developer")
                .status(ApplicationStatus.INTERVIEW)
                .applicationDate(LocalDate.now())
                .build();

        when(applicationRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(application));
        doNothing().when(mapper).updateEntity(any(), any());
        when(applicationRepository.save(any())).thenReturn(application);
        when(mapper.toResponse(any())).thenReturn(response);
        when(statusHistoryRepository.save(any())).thenReturn(StatusHistory.builder().build());
        doNothing().when(emailService).sendStatusChangeNotification(anyString(), anyString(), anyString(), anyString());

        service.update(1L, updateRequest, user);

        verify(statusHistoryRepository).save(any(StatusHistory.class));
        verify(emailService).sendStatusChangeNotification(
                eq(user.getEmail()), eq("Google"),
                eq("APPLIED"), eq("INTERVIEW"));
    }

    @Test
    @DisplayName("Should delete application for authorized user")
    void delete_Success() {
        when(applicationRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(application));

        service.delete(1L, user);

        verify(applicationRepository).delete(application);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent application")
    void delete_NotFound() {
        when(applicationRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
