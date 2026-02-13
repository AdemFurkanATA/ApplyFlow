package com.applyflow.scheduler;

import com.applyflow.entity.JobApplication;
import com.applyflow.enums.ApplicationStatus;
import com.applyflow.repository.JobApplicationRepository;
import com.applyflow.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final JobApplicationRepository applicationRepository;
    private final EmailService emailService;

    @Value("${application.reminder.enabled}")
    private boolean reminderEnabled;

    @Value("${application.reminder.stale-days}")
    private int staleDays;

    @Scheduled(cron = "0 0 9 * * MON-FRI")
    public void sendStaleApplicationReminders() {
        if (!reminderEnabled) {
            log.debug("Reminder scheduler is disabled");
            return;
        }

        log.info("Running stale application reminder check");

        LocalDateTime staleDate = LocalDateTime.now().minus(staleDays, ChronoUnit.DAYS);
        List<ApplicationStatus> terminalStatuses = List.of(
                ApplicationStatus.OFFER,
                ApplicationStatus.REJECTED);

        List<JobApplication> staleApplications = applicationRepository.findStaleApplications(terminalStatuses,
                staleDate);

        log.info("Found {} stale applications", staleApplications.size());

        for (JobApplication app : staleApplications) {
            emailService.sendReminderNotification(
                    app.getUser().getEmail(),
                    app.getCompanyName(),
                    app.getPosition(),
                    staleDays);
        }
    }
}
