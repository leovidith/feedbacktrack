package com.cognizant.training.feedbacktrack.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class RecognitionResponse {
    private Long recognitionId;
    private Long senderId;
    private Long targetUserId;
    private String badgeName;
    private String message;
}
