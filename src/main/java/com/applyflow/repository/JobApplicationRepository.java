package com.applyflow.repository;

import com.applyflow.entity.JobApplication;
import com.applyflow.entity.User;
import com.applyflow.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Page<JobApplication> findByUser(User user, Pageable pageable);

    Page<JobApplication> findByUserAndStatus(User user, ApplicationStatus status, Pageable pageable);

    @Query("SELECT j FROM JobApplication j WHERE j.user = :user " +
            "AND (:status IS NULL OR j.status = :status) " +
            "AND (:companyName IS NULL OR LOWER(j.companyName) LIKE LOWER(CONCAT('%', :companyName, '%'))) " +
            "AND (:startDate IS NULL OR j.applicationDate >= :startDate) " +
            "AND (:endDate IS NULL OR j.applicationDate <= :endDate)")
    Page<JobApplication> findByFilters(
            @Param("user") User user,
            @Param("status") ApplicationStatus status,
            @Param("companyName") String companyName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    Optional<JobApplication> findByIdAndUser(Long id, User user);

    @Query("SELECT j FROM JobApplication j WHERE j.status NOT IN :excludedStatuses " +
            "AND j.updatedAt < :staleDate")
    List<JobApplication> findStaleApplications(
            @Param("excludedStatuses") List<ApplicationStatus> excludedStatuses,
            @Param("staleDate") java.time.LocalDateTime staleDate);

    long countByUser(User user);

    long countByUserAndStatus(User user, ApplicationStatus status);
}
