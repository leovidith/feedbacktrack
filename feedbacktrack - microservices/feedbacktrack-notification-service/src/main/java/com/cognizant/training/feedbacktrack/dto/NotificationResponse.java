package com.cognizant.training.feedbacktrack.dto;

import com.cognizant.training.feedbacktrack.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {

    private Long notificationId;

    private NotificationType type;

    private String message;

    private Long sourceId;

    @JsonProperty("isRead")
    private boolean isRead;

    private LocalDateTime createdAt;
}