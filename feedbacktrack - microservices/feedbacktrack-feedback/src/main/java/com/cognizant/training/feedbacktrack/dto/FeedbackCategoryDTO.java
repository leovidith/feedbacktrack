package com.cognizant.training.feedbacktrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackCategoryDTO {
    private Long categoryId;
    private String categoryName;
    private String description;
}

