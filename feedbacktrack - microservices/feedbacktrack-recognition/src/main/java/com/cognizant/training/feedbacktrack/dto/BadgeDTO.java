package com.cognizant.training.feedbacktrack.dto;

import lombok.*;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class BadgeDTO {
    private Long badgeId;

    private String badgeName;
    private Integer pointsValue;
    private String description;
    private String badgeIconPath;
}
