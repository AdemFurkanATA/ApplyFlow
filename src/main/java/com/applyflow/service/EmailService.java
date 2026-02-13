package com.applyflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${application.mail.enabled}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:noreply@applyflow.com}")
    private String fromEmail;

    @Async
    public void sendStatusChangeNotification(String to, String companyName,
            String previousStatus, String newStatus) {
        String subject = "ApplyFlow - Application Status Updated";
        String body = String.format(
                "Your application status has been updated.%n%n" +
                        "Company: %s%n" +
                        "Previous Status: %s%n" +
                        "New Status: %s%n%n" +
                        "Log in to ApplyFlow for more details.",
                companyName, previousStatus, newStatus);

        sendEmail(to, subject, body);
    }

    @Async
    public void sendReminderNotification(String to, String companyName,
            String position, long staleDays) {
        String subject = "ApplyFlow - Application Reminder";
        String body = String.format(
                "Reminder: Your application has had no updates for %d days.%n%n" +
                        "Company: %s%n" +
                        "Position: %s%n%n" +
                        "Consider following up or updating the status in ApplyFlow.",
                staleDays, companyName, position);

        sendEmail(to, subject, body);
    }

    @Async
    public void sendWelcomeEmail(String to, String name) {
        String subject = "Welcome to ApplyFlow!";
        String body = String.format(
                "Hello %s,%n%n" +
                        "Welcome to ApplyFlow! Your account has been created successfully.%n%n" +
                        "Start tracking your job applications today.",
                name);

        sendEmail(to, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("Email sending disabled. Would send to={}, subject={}", to, subject);
            log.debug("Email body: {}", body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
