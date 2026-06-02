package com.cognizant.training.feedbacktrack.repository;


import com.cognizant.training.feedbacktrack.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);
}