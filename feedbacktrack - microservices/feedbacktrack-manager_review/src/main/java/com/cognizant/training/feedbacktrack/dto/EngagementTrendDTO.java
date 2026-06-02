package com.cognizant.training.feedbacktrack.dto;

import com.cognizant.training.feedbacktrack.enums.FeedbackAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EngagementTrendDTO {

    private Map<FeedbackAction, Long> actionBreakdown;

    private Double resolutionRatePercent;

    private List<MonthlyActivity> monthlyTrend;

    private long totalReviews;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyActivity {
        private int year;
        private int month;          // 1 = January … 12 = December
        private long reviewCount;
    }
}

