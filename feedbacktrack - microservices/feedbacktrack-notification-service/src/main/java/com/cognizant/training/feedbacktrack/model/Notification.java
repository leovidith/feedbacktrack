package com.cognizant.training.feedbacktrack.model;


import com.cognizant.training.feedbacktrack.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private Long sourceId; // Refers to FeedbackID or RecognitionID
    private String message;

    @JsonProperty("isRead")
    private boolean isRead = false;
    private LocalDateTime createdAt = LocalDateTime.now();
}
