package com.intellitrack.service;

import com.intellitrack.entity.Notification;
import com.intellitrack.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("null")
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired private AccessService access;

    public void createNotification(Long userId, String message) {
        // The existing notification column is VARCHAR(255). Long requirement names
        // must not cause the academic change transaction to fail.
        Notification notification = new Notification(userId, message.length()>255?message.substring(0,252)+"...":message);
        notificationRepository.save(notification);
    }

    public List<Notification> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if(!access.own(n.getUserId())) throw new org.springframework.security.access.AccessDeniedException("This notification belongs to another user");
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}
