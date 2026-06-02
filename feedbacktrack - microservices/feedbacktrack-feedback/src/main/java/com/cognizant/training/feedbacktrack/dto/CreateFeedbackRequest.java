package com.cognizant.training.feedbacktrack.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor @NoArgsConstructor
public class CreateFeedbackRequest {
//    @NotNull(message = "Sender ID cannot be null")
    private Long senderId;

    @NotNull(message = "Target User ID cannot be null")
    private Long targetUserId;

    @NotNull(message = "Category ID cannot be null")
    private Long categoryId;

    @NotNull(message = "Comments cannot be null")
    @Size(min = 10, max = 2000, message = "Comments must be between 10 and 2000 characters")
    private String comments;

    private boolean isAnonymous;
}