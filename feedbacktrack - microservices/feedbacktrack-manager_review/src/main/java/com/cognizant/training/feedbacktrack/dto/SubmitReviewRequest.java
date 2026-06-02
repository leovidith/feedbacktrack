package com.cognizant.training.feedbacktrack.dto;

import com.cognizant.training.feedbacktrack.enums.FeedbackAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitReviewRequest {
    @NotNull(message = "Feedback ID is required")
    @Positive(message = "Feedback ID must be a positive number")
    private Long feedbackId;

    private Long reviewerId;

    @NotNull(message = "An action must be specified")
    private FeedbackAction action;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}