package com.healthcare.emr;

import com.healthcare.emr.entity.Notification;
import com.healthcare.emr.entity.User;
import com.healthcare.emr.repository.UserRepository;
import com.healthcare.emr.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should send notifications, record dev email previews, and toggle read state")
    void testNotificationLifecycle() {
        User user = userRepository.findAll().get(0);

        // Send Notification
        Notification notif = notificationService.sendNotification(user, "Appointment Reminder", "Your checkup is scheduled tomorrow.", "REMINDER");
        assertNotNull(notif);
        assertNotNull(notif.getNotificationId());
        assertFalse(notif.isRead());

        // Get User Notifications
        List<Notification> userNotifs = notificationService.getUserNotifications(user.getUserId());
        assertFalse(userNotifs.isEmpty());

        // Mark as Read
        notificationService.markAsRead(notif.getNotificationId());
        List<Notification> updatedNotifs = notificationService.getUserNotifications(user.getUserId());
        Notification readNotif = updatedNotifs.stream()
                .filter(n -> n.getNotificationId().equals(notif.getNotificationId()))
                .findFirst()
                .orElse(null);
        assertNotNull(readNotif);
        assertTrue(readNotif.isRead());

        // Dev email previews
        List<Map<String, String>> previews = notificationService.getDevEmailPreviews();
        assertFalse(previews.isEmpty());
    }
}
