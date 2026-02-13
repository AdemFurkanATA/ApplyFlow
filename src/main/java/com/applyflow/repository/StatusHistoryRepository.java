package com.applyflow.repository;

import com.applyflow.entity.JobApplication;
import com.applyflow.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    List<StatusHistory> findByJobApplicationOrderByChangedAtDesc(JobApplication jobApplication);
}
