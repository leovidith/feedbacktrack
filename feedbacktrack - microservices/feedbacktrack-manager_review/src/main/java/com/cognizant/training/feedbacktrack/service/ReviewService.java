package com.cognizant.training.feedbacktrack.service;

import com.cognizant.training.feedbacktrack.enums.Role;
import com.cognizant.training.feedbacktrack.feign.FeedbackClient;
import com.cognizant.training.feedbacktrack.feign.UserClient;
import com.cognizant.training.feedbacktrack.dto.FeedbackResponseDTO;
import com.cognizant.training.feedbacktrack.dto.SubmitReviewRequest;
import com.cognizant.training.feedbacktrack.dto.UserResponseDTO;
import com.cognizant.training.feedbacktrack.enums.FeedbackAction;
import com.cognizant.training.feedbacktrack.exceptions.ReviewNotFoundException;
import com.cognizant.training.feedbacktrack.exceptions.UnauthorizedAccessException;
import com.cognizant.training.feedbacktrack.model.FeedbackReview;
import com.cognizant.training.feedbacktrack.repository.ReviewRepository;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int MAX_NOTES_LENGTH = 1000;
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final Set<FeedbackAction> ALLOWED_REVIEW_ACTIONS = Set.of(
            FeedbackAction.ACKNOWLEDGED,
            FeedbackAction.RESOLVED,
            FeedbackAction.PENDING
    );

    @Autowired
    private final ReviewRepository reviewRepository;

    @Autowired
    private final UserClient userClient;

    @Autowired
    private final FeedbackClient feedbackClient;

    @Transactional
    public FeedbackReview createReview(SubmitReviewRequest request) {
        validateSubmitRequest(request, true);

        if (reviewRepository.existsByFeedbackId(request.getFeedbackId())) {
            throw new IllegalStateException("A review already exists for this feedback.");
        }

        FeedbackResponseDTO feedback = feedbackClient.getFeedbackById(request.getFeedbackId());
        validateId(feedback.getTargetUserId(), "targetUserId");

        UserResponseDTO reviewer = fetchUserOrFail(request.getReviewerId());
        ensureManagerOrAdmin(reviewer, "Only managers or admins can submit a review.");

        if (!canManageTargetUser(reviewer, feedback.getTargetUserId())) {
            throw new UnauthorizedAccessException("Access Denied: You do not manage the target user.");
        }

        FeedbackReview review = new FeedbackReview();
        review.setFeedbackId(request.getFeedbackId());
        review.setReviewerId(request.getReviewerId());
        review.setActionTaken(request.getAction());
        review.setManagerNotes(normalizeNotes(request.getNotes()));
        review.setReviewDate(LocalDateTime.now());

        FeedbackReview saved = reviewRepository.save(review);
        log.info("Review created. reviewId={}, feedbackId={}, reviewerId={}",
                saved.getReviewId(), saved.getFeedbackId(), saved.getReviewerId());
        return saved;
    }

    public FeedbackReview getReviewDetails(Long reviewId, Long currentUserId) {
        validateId(reviewId, "reviewId");
        validateId(currentUserId, "currentUserId");

        FeedbackReview review = findReviewById(reviewId);
        FeedbackResponseDTO feedback = fetchFeedbackOrFail(review.getFeedbackId());
        UserResponseDTO currentUser = fetchUserOrFail(currentUserId);

        Long targetUserId = feedback.getTargetUserId();
        boolean isSender = currentUserId.equals(feedback.getSenderId());
        boolean isReviewer = review.getReviewerId().equals(currentUserId);
        boolean isTargetUser = currentUserId.equals(targetUserId);
        boolean isAdmin = hasRole(currentUser, ROLE_ADMIN);

        if (!isReviewer && !isTargetUser && !isAdmin && !isSender) {
            throw new UnauthorizedAccessException("Access Denied: You are not linked to this review.");
        }

        return review;
    }

    @Transactional
    public FeedbackReview updateReview(Long reviewId, SubmitReviewRequest request, Long currentUserId) {
        validateId(reviewId, "reviewId");
        validateId(currentUserId, "currentUserId");
        validateSubmitRequest(request, false);

        FeedbackReview existingReview = findReviewById(reviewId);
        UserResponseDTO currentUser = fetchUserOrFail(currentUserId);
        boolean isOwner = existingReview.getReviewerId().equals(currentUserId);
        boolean isAdmin = hasRole(currentUser, ROLE_ADMIN);
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedAccessException("Access Denied: You can only edit reviews you created.");
        }

        if (existingReview.getActionTaken() == FeedbackAction.RESOLVED) {
            throw new IllegalStateException("Resolved reviews cannot be modified.");
        }

        if (!existingReview.getFeedbackId().equals(request.getFeedbackId())) {
            throw new IllegalStateException("feedbackId cannot be changed for an existing review.");
        }
        if (!existingReview.getReviewerId().equals(request.getReviewerId())) {
            throw new IllegalStateException("reviewerId cannot be changed for an existing review.");
        }

        existingReview.setActionTaken(request.getAction());
        existingReview.setManagerNotes(normalizeNotes(request.getNotes()));

        FeedbackReview saved = reviewRepository.save(existingReview);
        log.info("Review updated. reviewId={}, action={}", saved.getReviewId(), saved.getActionTaken());
        return saved;
    }

    @Transactional
    public void deleteReview(Long reviewId, Long currentUserId) {
        validateId(reviewId, "reviewId");
        validateId(currentUserId, "currentUserId");

        FeedbackReview review = findReviewById(reviewId);
        UserResponseDTO currentUser = fetchUserOrFail(currentUserId);
        boolean isOwner = review.getReviewerId().equals(currentUserId);
        boolean isAdmin = hasRole(currentUser, ROLE_ADMIN);
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedAccessException("Access Denied: You can only delete reviews you created.");
        }

        reviewRepository.delete(review);
        log.info("Review deleted. reviewId={}", reviewId);
    }

    public List<FeedbackReview> getReviewsByReviewer(Long reviewerId) {
        validateId(reviewerId, "reviewerId");
        UserResponseDTO requester = fetchUserOrFail(getCurrentUserId());
        UserResponseDTO reviewer = fetchUserOrFail(reviewerId);
        if(!(requester.getRole().equals(Role.MANAGER) && requester.getUserId().equals(reviewerId)) && !hasRole(requester, ROLE_ADMIN)) {
            throw new UnauthorizedAccessException("Access Denied: You can only view your own reviews.");
        }
        return reviewRepository.findAllByReviewerIdOrderByReviewDateDesc(reviewerId);
    }

    public List<FeedbackReview> getPendingReviews(Long reviewerId) {
        validateId(reviewerId, "reviewerId");
        UserResponseDTO requester = fetchUserOrFail(getCurrentUserId());
        UserResponseDTO reviewer = fetchUserOrFail(reviewerId);
        if(!(requester.getRole().equals(Role.MANAGER) && requester.getUserId().equals(reviewerId)) && !hasRole(requester, ROLE_ADMIN)) {
            throw new UnauthorizedAccessException("Access Denied: You can only view your own reviews.");
        }

        return reviewRepository.findAllByReviewerIdAndActionTakenNot(reviewerId, FeedbackAction.RESOLVED);
    }

    public FeedbackReview getReviewByFeedbackId(Long feedbackId, Long currentUserId) {
        validateId(feedbackId, "feedbackId");
        validateId(currentUserId, "currentUserId");

        FeedbackReview review = reviewRepository.findByFeedbackId(feedbackId)
                .orElseThrow(() -> new ReviewNotFoundException("Review with feedbackId " + feedbackId + " not found."));

        FeedbackResponseDTO feedback = fetchFeedbackOrFail(feedbackId);
        UserResponseDTO currentUser = fetchUserOrFail(currentUserId);

        boolean isReviewer = review.getReviewerId().equals(currentUserId);
        boolean isTargetUser = currentUserId.equals(feedback.getTargetUserId());
        boolean isAdmin = hasRole(currentUser, ROLE_ADMIN);
        boolean isSender = currentUserId.equals(feedback.getSenderId());
        if (!isReviewer && !isTargetUser && !isAdmin && !isSender) {
            throw new UnauthorizedAccessException("Access Denied: You cannot view this review.");
        }

        return review;
    }

    public List<FeedbackReview> getAllReviews(Long currentUserId) {
        validateId(currentUserId, "currentUserId");
        UserResponseDTO requester = fetchUserOrFail(currentUserId);
        if (!hasRole(requester, ROLE_ADMIN)) {
            throw new UnauthorizedAccessException("Access Denied: Admin role required.");
        }

        return reviewRepository.findAll();
    }

    private FeedbackReview findReviewById(Long reviewId) {
        FeedbackReview feedbackReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));
        return feedbackReview;
    }

    private FeedbackResponseDTO fetchFeedbackOrFail(Long feedbackId) {
        try {
            FeedbackResponseDTO response = feedbackClient.getFeedbackById(feedbackId);
            if (response == null) {
                throw new IllegalStateException("Feedback service returned an empty response for feedbackId " + feedbackId + ".");
            }
            if (response.getFeedbackId() == null) {
                response.setFeedbackId(feedbackId);
            }
            return response;
        } catch (FeignException.NotFound ex) {
            throw new IllegalStateException("Feedback with id " + feedbackId + " does not exist.");
        } catch (FeignException ex) {
            throw new IllegalStateException("Unable to validate feedback due to downstream service error.");
        }
    }

    private UserResponseDTO fetchUserOrFail(Long userId) {
        try {
            UserResponseDTO response = userClient.getUserById(userId);
            if (response == null) {
                throw new IllegalStateException("User service returned an empty response for userId " + userId + ".");
            }
            if (response.getUserId() == null) {
                response.setUserId(userId);
            }
            return response;
        } catch (FeignException.NotFound ex) {
            throw new IllegalStateException("User with id " + userId + " does not exist.");
        } catch (FeignException ex) {
            throw new IllegalStateException("Unable to validate user due to downstream service error.");
        }
    }

    private void ensureManagerOrAdmin(UserResponseDTO user, String message) {
        if (!hasRole(user, ROLE_MANAGER) && !hasRole(user, ROLE_ADMIN)) {
            throw new UnauthorizedAccessException("Access Denied: " + message);
        }
    }

    private boolean canManageTargetUser(UserResponseDTO reviewer, Long targetUserId) {
        if (hasRole(reviewer, ROLE_ADMIN)) {
            return true;
        }
        if (!hasRole(reviewer, ROLE_MANAGER)) {
            return false;
        }

        UserResponseDTO targetUser = fetchUserOrFail(targetUserId);
        return targetUser.getManagerId() != null && targetUser.getManagerId().equals(reviewer.getUserId());
    }

    private boolean hasRole(UserResponseDTO user, String expectedRole) {
        return user != null
                && user.getRole() != null
                && expectedRole.equalsIgnoreCase(user.getRole().toString().trim());
    }

    private void validateSubmitRequest(SubmitReviewRequest request, boolean isCreate) {
        if (request == null) {
            throw new IllegalStateException("Request body is required.");
        }

        validateId(request.getFeedbackId(), "feedbackId");
        validateId(request.getReviewerId(), "reviewerId");

        if (request.getAction() == null) {
            throw new IllegalStateException("action is required.");
        }
        if (!ALLOWED_REVIEW_ACTIONS.contains(request.getAction())) {
            throw new IllegalStateException("Only ACKNOWLEDGED or RESOLVED actions are allowed.");
        }

        String normalizedNotes = normalizeNotes(request.getNotes());
        if (normalizedNotes != null && normalizedNotes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalStateException("notes cannot exceed " + MAX_NOTES_LENGTH + " characters.");
        }

        if (isCreate && request.getAction() == FeedbackAction.RESOLVED) {
            throw new IllegalStateException("A new review cannot be created directly as RESOLVED.");
        }
    }

    private void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalStateException(fieldName + " must be a positive number.");
        }
    }

    private String normalizeNotes(String notes) {
        if (notes == null) {
            return null;
        }
        String trimmed = notes.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Authenticated user is required.");
        }
        return Long.parseLong(authentication.getPrincipal().toString());
    }
}
