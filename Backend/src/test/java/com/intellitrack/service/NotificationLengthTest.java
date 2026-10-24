package com.intellitrack.service;

import com.intellitrack.entity.Notification;
import com.intellitrack.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationLengthTest {
    @Test void longRequirementNamesFitExistingNotificationColumn() {
        var repository=mock(NotificationRepository.class);
        var service=new NotificationService();
        ReflectionTestUtils.setField(service,"notificationRepository",repository);
        service.createNotification(1L,"New requirement: "+"x".repeat(300));
        var notification=ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(notification.capture());
        assertEquals(255,notification.getValue().getMessage().length());
        assertTrue(notification.getValue().getMessage().startsWith("New requirement:"));
        assertEquals(1L,notification.getValue().getUserId());
    }
}
