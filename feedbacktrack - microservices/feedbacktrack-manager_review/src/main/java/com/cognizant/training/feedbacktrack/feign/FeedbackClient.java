package com.cognizant.training.feedbacktrack.feign;

import com.cognizant.training.feedbacktrack.config.FeignClientConfig;
import com.cognizant.training.feedbacktrack.dto.FeedbackResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "feedbacktrack-feedback-service", configuration = FeignClientConfig.class)
public interface FeedbackClient {

    @GetMapping("/api/v1/feedback/{id}")
    FeedbackResponseDTO getFeedbackById(@PathVariable("id") Long feedbackId);

    @GetMapping("/api/v1/feedback/count/target/{targetUserId}")
    long countByTargetUserId(@PathVariable("targetUserId") Long targetUserId);
}


