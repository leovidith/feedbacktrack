package com.cognizant.training.feedbacktrack.controller;

import com.cognizant.training.feedbacktrack.dto.SubmitReviewRequest;
import com.cognizant.training.feedbacktrack.model.FeedbackReview;
import com.cognizant.training.feedbacktrack.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manager/reviews")
@RequiredArgsConstructor
@Tag(name = "Review Management", description = "Endpoints for managers to create, update, and manage feedback reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Authenticated user is required.");
        }
        return Long.parseLong(authentication.getPrincipal().toString());
    }

    @Operation(summary = "Create a new review", description = "Submits a review for a specific feedback entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @PostMapping("/create")
    public ResponseEntity<FeedbackReview> createReview(@Valid @RequestBody SubmitReviewRequest request) {
        Long currentUserId = getCurrentUserId();
        request.setReviewerId(currentUserId);
        FeedbackReview review = reviewService.createReview(request);
        return new ResponseEntity<>(review, HttpStatus.CREATED);
    }

    @Operation(summary = "Get review details by ID", description = "Retrieves the full details of a specific review.")
    @GetMapping("/{id}")
    public ResponseEntity<FeedbackReview> getReviewDetails(@PathVariable("id") Long id) {
        Long currentUserId = getCurrentUserId();
        FeedbackReview review = reviewService.getReviewDetails(id, currentUserId);
        return ResponseEntity.ok(review);
    }

    @Operation(summary = "Update an existing review", description = "Updates the content or status of a previously submitted review.")
    @PutMapping("/{id}")
    public ResponseEntity<FeedbackReview> updateReview(
            @PathVariable("id") Long id,
            @Valid @RequestBody SubmitReviewRequest updateRequest) {
        Long currentUserId = getCurrentUserId();
        updateRequest.setReviewerId(currentUserId);
        FeedbackReview updatedReview = reviewService.updateReview(id, updateRequest, currentUserId);
        return ResponseEntity.ok(updatedReview);
    }

    @Operation(summary = "Delete a review", description = "Removes a review from the system.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        reviewService.deleteReview(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get review history by reviewer", description = "Fetches all reviews submitted by a specific reviewer ID.")
    @GetMapping("/reviewer/{id}")
    public ResponseEntity<List<FeedbackReview>> getReviewerHistory(@PathVariable("id") Long reviewerId) {
        return ResponseEntity.ok(reviewService.getReviewsByReviewer(reviewerId));
    }

    @Operation(summary = "Get pending reviews", description = "Fetches reviews that are currently in pending status for a specific reviewer.")
    @GetMapping("/reviewer/pending/{id}")
    public ResponseEntity<List<FeedbackReview>> getPendingReviews(@PathVariable("id") Long reviewerId) {
        return ResponseEntity.ok(reviewService.getPendingReviews(reviewerId));
    }

    @Operation(summary = "Get review by feedback ID", description = "Retrieves the review associated with a specific feedback submission.")
    @GetMapping("/feedback/{feedbackId}")
    public ResponseEntity<FeedbackReview> getReviewByFeedbackId(@PathVariable("feedbackId") Long feedbackId) {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(reviewService.getReviewByFeedbackId(feedbackId, currentUserId));
    }

    @Operation(summary = "Get all reviews", description = "Retrieves a list of all reviews visible to the current manager.")
    @GetMapping("/all")
    public ResponseEntity<List<FeedbackReview>> getAllReviews() {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(reviewService.getAllReviews(currentUserId));
    }
}
