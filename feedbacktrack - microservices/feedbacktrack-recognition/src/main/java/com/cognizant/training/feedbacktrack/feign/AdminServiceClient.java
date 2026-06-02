package com.cognizant.training.feedbacktrack.feign;

import com.cognizant.training.feedbacktrack.dto.BadgeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// ⚠️ Change "feedbacktrack-admin-service" to match spring.application.name of your Admin microservice
@FeignClient(name = "feedbacktrack-auth-service", contextId = "AdminServiceClient")
public interface AdminServiceClient {

    // Validates that a badge exists; used in create()
    @GetMapping("/api/admin/badges/{id}")
    BadgeDTO getBadgeById(@PathVariable("id") Long badgeId);
}
