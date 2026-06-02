package com.cognizant.training.feedbacktrack.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Builder
public class FeedbackResponseDTO {
    private Long feedbackId;
    private Long senderId;
    private Long targetUserId;
    private Long categoryId;
    private String comments;
    private boolean isAnonymous;
    private LocalDateTime submittedDate;
}