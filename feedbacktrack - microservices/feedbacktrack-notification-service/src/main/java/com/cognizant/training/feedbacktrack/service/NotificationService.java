package com.cognizant.training.feedbacktrack.service;


import com.cognizant.training.feedbacktrack.client.UserClient;
import com.cognizant.training.feedbacktrack.dto.NotificationResponse;
import com.cognizant.training.feedbacktrack.dto.UserResponseDTO;
import com.cognizant.training.feedbacktrack.enums.NotificationType;
import com.cognizant.training.feedbacktrack.exception.ResourceNotFoundException;
import com.cognizant.training.feedbacktrack.model.Notification;
import com.cognizant.training.feedbacktrack.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository repo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserClient userClient;

    public void sendNotification(Long userId, String message, NotificationType type,Long sourceId) {
        log.info("Sending notification to userId: {}",userId);

        try {
            UserResponseDTO exists = userClient.getUserById(userId);

            if (exists==null || exists.getUserId()==null) {
                throw new ResourceNotFoundException("User not found");
            }

        } catch (feign.FeignException e) {
            log.error("User service unavailable");
            throw new RuntimeException("User service unavailable");
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setSourceId(sourceId);
        notification.setType(type);

        repo.save(notification);

        log.info("Notification saved in DB for UserId: {}",userId);

        try{
            messagingTemplate.convertAndSend(
                    "/topic/api/v1/notifications/" + userId,
                    notification
            );
            log.info("Websocket notification sent to userId: {}",userId);
        }catch (Exception e){
            log.error("Websocket delivery failed for UserId: {}",userId);
            throw new ResourceNotFoundException("WebSocket delivery failed");
        }
    }

    public List<NotificationResponse> getUserNotifications(Long userId) {
        log.info("Fetching notifications for userId: {}",userId);

        try {
            UserResponseDTO exists = userClient.getUserById(userId);

            if (exists==null || exists.getUserId()==null) {
                throw new ResourceNotFoundException("User not found");
            }

        } catch (feign.FeignException e) {
            log.error("User service unavailable");
            throw new RuntimeException("User service unavailable");
        }

        log.info("Notifications fetched for UserId: {}",userId);
        List<NotificationResponse> res = repo.findByUserId(userId).stream()
                .map(notify -> new NotificationResponse(
                        notify.getNotificationId(),
                        notify.getType(),
                        notify.getMessage(),
                        notify.getSourceId(),
                        notify.isRead(),
                        notify.getCreatedAt()
                )).toList();
        return res;

    }

    public void markAsRead(Long id) {
        log.info("Marking notification as read, id: {}",id);
        Notification notif = repo.findById(id)
                .orElseThrow(()->{
                    log.error("Notification not found with id: {}",id);
                    return new ResourceNotFoundException("Notification not found");
        } );
        notif.setRead(true);
        repo.save(notif);
        log.info("Notification marked as read id: {}",id);
    }

}