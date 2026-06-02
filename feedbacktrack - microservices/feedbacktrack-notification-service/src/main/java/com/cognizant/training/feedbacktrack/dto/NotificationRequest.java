package com.cognizant.training.feedbacktrack.dto;

import com.cognizant.training.feedbacktrack.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String message;

    @NotNull
    private NotificationType type;

    private Long sourceId;
}