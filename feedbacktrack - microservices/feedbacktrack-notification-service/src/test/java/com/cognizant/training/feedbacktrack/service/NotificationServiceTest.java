package com.cognizant.training.feedbacktrack.service;

import com.cognizant.training.feedbacktrack.client.UserClient;
import com.cognizant.training.feedbacktrack.dto.NotificationResponse;
import com.cognizant.training.feedbacktrack.dto.UserResponseDTO;
import com.cognizant.training.feedbacktrack.enums.NotificationType;
import com.cognizant.training.feedbacktrack.exception.ResourceNotFoundException;
import com.cognizant.training.feedbacktrack.model.Notification;
import com.cognizant.training.feedbacktrack.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repo;

    @Mock
    private UserClient userClient;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService service;

    @Test
    void sendNotification_shouldSaveAndSend_whenUserExists() {

        Long userId = 1L;
        String message = "Test message";

        UserResponseDTO mockUser = new UserResponseDTO();
        mockUser.setUserId(userId); // ← populate the id field so service validation passes

        when(userClient.getUserById(userId)).thenReturn(mockUser);
        when(repo.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.sendNotification(userId, message, NotificationType.FEEDBACK_RECEIVED,1L);

        verify(repo, times(1)).save(any(Notification.class));
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/api/v1/notifications/" + userId),
                        any(Notification.class));
    }

    @Test
    void sendNotification_shouldThrowException_whenUserNotExists() {

        Long userId = 1L;
        when(userClient.getUserById(userId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () ->
                service.sendNotification(userId, "Test", NotificationType.FEEDBACK_RECEIVED,1L));
    }

    @Test
    void getUserNotifications_shouldReturnList_whenUserExists() {

        Long userId = 1L;

        UserResponseDTO mockUser = new UserResponseDTO();
        mockUser.setUserId(userId); // ← populate the id field so service validation passes

        when(userClient.getUserById(userId)).thenReturn(mockUser);

        List<Notification> list = List.of(new Notification());
        when(repo.findByUserId(userId)).thenReturn(list);

        List<NotificationResponse> result = service.getUserNotifications(userId);

        assertEquals(1, result.size());
    }

    @Test
    void getUserNotifications_shouldThrowException_whenUserNotExists() {

        Long userId = 1L;
        when(userClient.getUserById(userId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () ->
                service.getUserNotifications(userId));
    }

    @Test
    void markAsRead_shouldUpdateNotification() {

        Notification notif = new Notification();
        notif.setNotificationId(1L);
        notif.setRead(false);

        when(repo.findById(1L)).thenReturn(Optional.of(notif));

        service.markAsRead(1L);

        assertTrue(notif.isRead());
        verify(repo).save(notif);
    }

    @Test
    void markAsRead_shouldThrowException_whenNotFound() {

        when(repo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.markAsRead(1L));
    }
}