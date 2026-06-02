package com.cognizant.training.feedbacktrack.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Data
public class CreateRecognitionRequest {
    @NotNull
    private Long targetUserId;
    @NotNull
    private Long badgeId;
    private String message;
}
