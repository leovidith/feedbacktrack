package com.cognizant.training.feedbacktrack.feign;

import com.cognizant.training.feedbacktrack.dto.FeedbackCategoryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "feedbacktrack-auth-service", contextId = "FeedbackCategoryClient")
public interface FeedbackCategoryClient {

    @GetMapping("/api/admin/categories/{id}")
    FeedbackCategoryDTO getCategoryById(@PathVariable("id") Long id);

    @GetMapping("/api/admin/categories/all")
    List<FeedbackCategoryDTO> getAllCategories();
}