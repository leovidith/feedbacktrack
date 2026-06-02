package com.cognizant.training.feedbacktrack.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.cognizant.training.feedbacktrack.dto.*;
import com.cognizant.training.feedbacktrack.enums.*;
import com.cognizant.training.feedbacktrack.exceptions.*;
import com.cognizant.training.feedbacktrack.feign.*;
import com.cognizant.training.feedbacktrack.model.FeedbackReview;
import com.cognizant.training.feedbacktrack.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private UserClient userClient;
    @Mock private FeedbackClient feedbackClient;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private ReviewService reviewService;

    private UserResponseDTO manager;
    private UserResponseDTO employee;
    private FeedbackResponseDTO feedback;
    private SubmitReviewRequest request;

    @BeforeEach
    void setup() {
        manager = new UserResponseDTO();
        manager.setUserId(1L);
        manager.setRole(Role.MANAGER);

        employee = new UserResponseDTO();
        employee.setUserId(2L);
        employee.setManagerId(1L);
        employee.setRole(Role.EMPLOYEE);

        feedback = new FeedbackResponseDTO();
        feedback.setFeedbackId(10L);
        feedback.setTargetUserId(2L);
        feedback.setSenderId(3L);

        request = new SubmitReviewRequest();
        request.setFeedbackId(10L);
        request.setReviewerId(1L);
        request.setAction(FeedbackAction.ACKNOWLEDGED);
        request.setNotes("Processing feedback.");
    }


    @Test
    @DisplayName("createReview: Success path for Manager")
    void createReview_Success() {
        when(reviewRepository.existsByFeedbackId(10L)).thenReturn(false);
        when(feedbackClient.getFeedbackById(10L)).thenReturn(feedback);
        when(userClient.getUserById(1L)).thenReturn(manager);
        when(userClient.getUserById(2L)).thenReturn(employee);
        when(reviewRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        FeedbackReview result = reviewService.createReview(request);

        assertNotNull(result);
        assertEquals(FeedbackAction.ACKNOWLEDGED, result.getActionTaken());
        verify(reviewRepository).save(any());
    }

    @Test
    @DisplayName("createReview: Fails if review already exists")
    void createReview_Exists() {
        when(reviewRepository.existsByFeedbackId(10L)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> reviewService.createReview(request));
    }

    @Test
    @DisplayName("createReview: Fails if Manager doesn't manage Target User")
    void createReview_NotManagerOfTarget() {
        employee.setManagerId(99L); // Different manager
        when(reviewRepository.existsByFeedbackId(10L)).thenReturn(false);
        when(feedbackClient.getFeedbackById(10L)).thenReturn(feedback);
        when(userClient.getUserById(1L)).thenReturn(manager);
        when(userClient.getUserById(2L)).thenReturn(employee);

        assertThrows(UnauthorizedAccessException.class, () -> reviewService.createReview(request));
    }

    // --- UPDATE REVIEW TESTS ---

    @Test
    @DisplayName("updateReview: Success path")
    void updateReview_Success() {
        FeedbackReview existing = new FeedbackReview();
        existing.setReviewerId(1L);
        existing.setFeedbackId(10L);
        existing.setActionTaken(FeedbackAction.PENDING);

        when(reviewRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(userClient.getUserById(1L)).thenReturn(manager);
        when(reviewRepository.save(any())).thenReturn(existing);

        FeedbackReview result = reviewService.updateReview(50L, request, 1L);

        assertEquals(FeedbackAction.ACKNOWLEDGED, result.getActionTaken());
    }

    @Test
    @DisplayName("updateReview: Fails if already RESOLVED")
    void updateReview_ResolvedFail() {
        FeedbackReview existing = new FeedbackReview();
        existing.setReviewerId(1L);
        existing.setActionTaken(FeedbackAction.RESOLVED);

        when(reviewRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(userClient.getUserById(1L)).thenReturn(manager);

        assertThrows(IllegalStateException.class, () -> reviewService.updateReview(50L, request, 1L));
    }

    // --- GET DETAILS TESTS ---

    @Test
    @DisplayName("getReviewDetails: Fails if user is not linked to review")
    void getReviewDetails_Unauthorized() {
        FeedbackReview review = new FeedbackReview();
        review.setReviewerId(1L);
        review.setFeedbackId(10L);

        UserResponseDTO stranger = new UserResponseDTO();
        stranger.setUserId(99L);
        stranger.setRole(Role.EMPLOYEE);

        when(reviewRepository.findById(50L)).thenReturn(Optional.of(review));
        when(feedbackClient.getFeedbackById(10L)).thenReturn(feedback);
        when(userClient.getUserById(99L)).thenReturn(stranger);

        assertThrows(UnauthorizedAccessException.class, () -> reviewService.getReviewDetails(50L, 99L));
    }

    // --- SECURITY CONTEXT TESTS (getCurrentUserId) ---

    @Test
    @DisplayName("getPendingReviews: Success for owner")
    void getPendingReviews_Success() {
        // Mocking static SecurityContextHolder
        try (MockedStatic<SecurityContextHolder> mockedContext = mockStatic(SecurityContextHolder.class)) {
            mockedContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn("1");

            when(userClient.getUserById(1L)).thenReturn(manager);

            reviewService.getPendingReviews(1L);
            verify(reviewRepository).findAllByReviewerIdAndActionTakenNot(1L, FeedbackAction.RESOLVED);
        }
    }
}