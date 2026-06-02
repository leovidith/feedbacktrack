package com.cognizant.training.feedbacktrack.controller;

import com.cognizant.training.feedbacktrack.dto.EngagementTrendDTO;
import com.cognizant.training.feedbacktrack.service.EngagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/manager/engagement")
@RequiredArgsConstructor
public class EngagementController {

    @Autowired
    private final EngagementService engagementService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(authentication.getPrincipal().toString());
    }

    @GetMapping("/trend/reviewer/{reviewerId}")
    public ResponseEntity<EngagementTrendDTO> getReviewerTrend(
            @PathVariable Long reviewerId,
            @RequestParam(value = "months", required = false, defaultValue = "6") Integer months) {
        // TODO: extract reviewerId from JWT/SecurityContext (manager self-service)
        return ResponseEntity.ok(engagementService.getTrendForReviewer(reviewerId, months));
    }

    @GetMapping("/trend/global")
    public ResponseEntity<EngagementTrendDTO> getGlobalTrend(
            @RequestParam(value = "months", required = false, defaultValue = "6") Integer months) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(engagementService.getGlobalTrend(userId, months));
    }
}
