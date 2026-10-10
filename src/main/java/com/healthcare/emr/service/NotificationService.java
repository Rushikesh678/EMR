package com.healthcare.emr.service;

import com.healthcare.emr.entity.Notification;
import com.healthcare.emr.entity.User;
import com.healthcare.emr.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    @Value("${app.mail.dev-mode:true}")
    private boolean devMode;

    @Value("${app.mail.sender:noreply@healthcare.local}")
    private String senderEmail;

    // In-memory dev email preview log for demo/testing
    private final List<Map<String, String>> devEmailPreviews = Collections.synchronizedList(new ArrayList<>());

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification sendNotification(User user, String title, String message, String type) {
        Notification notification = new Notification(user, title, message, type);
        Notification saved = notificationRepository.save(notification);

        // Development mode email simulation
        sendEmail(user.getEmail(), title, message);

        return saved;
    }

    public void sendEmail(String toEmail, String subject, String body) {
        if (devMode) {
            log.info("[DEV EMAIL PREVIEW] To: {} | Subject: {} | Body: {}", toEmail, subject, body);
            Map<String, String> emailRecord = new ConcurrentHashMap<>();
            emailRecord.put("to", toEmail);
            emailRecord.put("subject", subject);
            emailRecord.put("body", body);
            emailRecord.put("status", "SIMULATED_DEV_MODE");
            emailRecord.put("timestamp", java.time.LocalDateTime.now().toString());

            devEmailPreviews.add(0, emailRecord);
            if (devEmailPreviews.size() > 50) {
                devEmailPreviews.remove(devEmailPreviews.size() - 1);
            }
        } else {
            // Real SMTP provider would be invoked here when configured
            log.info("Production SMTP would dispatch email to {}", toEmail);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public List<Map<String, String>> getDevEmailPreviews() {
        return new ArrayList<>(devEmailPreviews);
    }
}
