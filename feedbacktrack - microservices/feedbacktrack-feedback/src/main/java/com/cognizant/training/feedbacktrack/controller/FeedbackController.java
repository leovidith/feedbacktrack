package com.cognizant.training.feedbacktrack.controller;

import com.cognizant.training.feedbacktrack.dto.CreateFeedbackRequest;
import com.cognizant.training.feedbacktrack.dto.FeedbackResponseDTO;
import com.cognizant.training.feedbacktrack.exceptions.UnauthorizedAccessException;
import com.cognizant.training.feedbacktrack.model.Feedback;
import com.cognizant.training.feedbacktrack.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedback")
@Slf4j
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<Feedback> createNewFeedback(@Valid @RequestBody CreateFeedbackRequest request) {
        Long currentUserId = getCurrentUserId();
        request.setSenderId(currentUserId);
        log.info("REST request to create new feedback for target user ID: {} by user ID: {}", request.getTargetUserId(), currentUserId);
        Feedback savedFeedback = feedbackService.createNewFeedback(request);
        return new ResponseEntity<>(savedFeedback, HttpStatus.CREATED);
    }

    @GetMapping("/sent")
    public ResponseEntity<List<FeedbackResponseDTO>> getMySentFeedbacks() {
        Long currentUserId = getCurrentUserId();
        log.info("REST request to fetch feedback sent by user ID: {}", currentUserId);
        List<FeedbackResponseDTO> list = feedbackService.getFeedbacksSentBy(currentUserId)
                .stream()
                .map(FeedbackController::toSenderViewDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/received")
    public ResponseEntity<List<FeedbackResponseDTO>> getMyReceivedFeedbacks() {
        Long currentUserId = getCurrentUserId();
        log.info("REST request to fetch feedback received by user ID: {}", currentUserId);
        List<FeedbackResponseDTO> list = feedbackService.getFeedbacksReceivedBy(currentUserId)
                .stream()
                .map(FeedbackController::toResponseDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackResponseDTO> getFeedbackById(@PathVariable Long id) {
        log.info("REST request to get feedback with ID: {}", id);
        FeedbackResponseDTO responseDTO = toResponseDTO(feedbackService.getFeedbackById(id));
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedbackById(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        log.info("REST request to delete feedback ID: {} by user ID: {}", id, currentUserId);
        feedbackService.deleteFeedbackById(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/manager/{managerId}")
    public ResponseEntity<List<Feedback>> getManagerFeedbacks(@PathVariable Long managerId) {
        log.info("REST request to get team feedback for manager ID: {}", managerId);
        List<Feedback> feedbacks = feedbackService.getManagerFeedbacks(managerId);
        return ResponseEntity.ok(feedbacks); //change this
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Feedback> toggleVisibility(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        log.info("REST request to toggle visibility for feedback ID: {} by user ID: {}", id, currentUserId);
        Feedback feedback = feedbackService.toggleVisibility(id, currentUserId);
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/count/target/{targetUserId}")
    public long countByTargetUserId(@PathVariable("targetUserId") Long targetUserId) {
        log.info("REST request to count feedback for target user ID: {}", targetUserId);
        return feedbackService.countByTargetUserId(targetUserId); //change this
    }

    @GetMapping("/all")
    public ResponseEntity<List<Feedback>> getAll(){
        Long currentUserId = getCurrentUserId();
        List<Feedback> fs = feedbackService.getAll(currentUserId);
        return new ResponseEntity<>(fs, HttpStatus.OK);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            throw new UnauthorizedAccessException("User is not authenticated.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof Integer userId) {
            return userId.longValue();
        }
        if (principal instanceof String userId) {
            try {
                return Long.parseLong(userId);
            } catch (NumberFormatException ex) {
                throw new UnauthorizedAccessException("Invalid authenticated user id.");
            }
        }

        throw new UnauthorizedAccessException("Unsupported authentication principal type.");
    }

    private static FeedbackResponseDTO toResponseDTO(Feedback feedback) {
        if (feedback == null) {
            return null;
        }

        return FeedbackResponseDTO.builder()
                .feedbackId(feedback.getFeedbackId())
                .senderId(feedback.isAnonymous() ? null : feedback.getSenderId())
                .targetUserId(feedback.getTargetUserId())
                .categoryId(feedback.getCategoryId())
                .comments(feedback.getComments())
                .isAnonymous(feedback.isAnonymous())
                .submittedDate(feedback.getSubmittedDate())
                .build();
    }

    private static FeedbackResponseDTO toSenderViewDTO(Feedback feedback) {
        if (feedback == null) {
            return null;
        }
        return FeedbackResponseDTO.builder()
                .feedbackId(feedback.getFeedbackId())
                .senderId(feedback.getSenderId())
                .targetUserId(feedback.getTargetUserId())
                .categoryId(feedback.getCategoryId())
                .comments(feedback.getComments())
                .isAnonymous(feedback.isAnonymous())
                .submittedDate(feedback.getSubmittedDate())
                .build();
    }
}