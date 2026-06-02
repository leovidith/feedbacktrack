package com.cognizant.training.feedbacktrack.service;

import com.cognizant.training.feedbacktrack.dto.CreateFeedbackRequest;
import com.cognizant.training.feedbacktrack.dto.FeedbackCategoryDTO;
import com.cognizant.training.feedbacktrack.dto.UserDTO;
import com.cognizant.training.feedbacktrack.enums.Role;
import com.cognizant.training.feedbacktrack.exceptions.*;
import com.cognizant.training.feedbacktrack.feign.FeedbackCategoryClient;
import com.cognizant.training.feedbacktrack.feign.UserServiceClient;
import com.cognizant.training.feedbacktrack.model.Feedback;
import com.cognizant.training.feedbacktrack.repository.FeedbackRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor @NoArgsConstructor
@Slf4j
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private FeedbackCategoryClient feedbackCategoryClient;

    @Autowired
    private UserServiceClient userServiceClient;

    @Transactional
    public Feedback createNewFeedback(CreateFeedbackRequest request) {
        log.info("Processing feedback from {} to {}", request.getSenderId(), request.getTargetUserId());

        // Business Rule: Users cannot give feedback to themselves
        if (request.getSenderId().equals(request.getTargetUserId())) {
            throw new InvalidFeedbackException("You cannot submit feedback for yourself.");
        }

        // Validate sender exists via OpenFeign inter-service call to User Service
        UserDTO sender = userServiceClient.getUserById(request.getSenderId());
        if (sender == null) {
            throw new UserNotFoundException("User not found with ID: " + request.getSenderId());
        }

        // Validate target user exists via OpenFeign inter-service call to User Service
        UserDTO targetUser = userServiceClient.getUserById(request.getTargetUserId());
        if (targetUser == null) {
            throw new UserNotFoundException("Target user not found with ID: " + request.getTargetUserId());
        }

        // Validate category exists via OpenFeign inter-service call to Category Service
        FeedbackCategoryDTO category = feedbackCategoryClient.getCategoryById(request.getCategoryId());
        if (category == null) {
            throw new FeedbackCategoryNotFoundException("Category not found with ID: " + request.getCategoryId());
        }

        Feedback feedback = new Feedback();
        feedback.setSenderId(request.getSenderId());
        feedback.setTargetUserId(request.getTargetUserId());
        feedback.setCategoryId(request.getCategoryId());
        feedback.setComments(request.getComments());
        feedback.setAnonymous(request.isAnonymous());
        feedback.setSubmittedDate(LocalDateTime.now());

        return feedbackRepository.save(feedback);
    }

    public Feedback getFeedbackById(Long id) {
        Long currentUserId = getCurrentUserId();
        UserDTO requester = userServiceClient.getUserById(currentUserId);
        Feedback feedback=feedbackRepository.findById(id)
                .orElseThrow(() -> new FeedbackNotFoundException("Feedback not found with ID: " + id));
        if (!requester.getRole().equals(Role.ADMIN) && !requester.getRole().equals(Role.MANAGER)) {
            if (!feedback.getTargetUserId().equals(currentUserId) && !feedback.getSenderId().equals(currentUserId)) {
                log.warn("Unauthorized access attempt to view feedback ID {} by user {}", id, currentUserId);
                throw new UnauthorizedAccessException("You can only view feedback where you are the sender or recipient.");
            }
        }
        return feedback;
    }

    @Transactional
    public void deleteFeedbackById(Long feedbackId, Long currentUserId) {
        Feedback feedback = getFeedbackById(feedbackId);

        if (!feedback.getSenderId().equals(currentUserId)) {
            throw new UnauthorizedAccessException("You can only delete your own feedback.");
        }
        feedbackRepository.delete(feedback);
        log.info("Feedback {} deleted by user {}", feedbackId, currentUserId);
    }

    public List<Feedback> getManagerFeedbacks(Long managerId) {
        log.info("Fetching team feedback for manager: {}", managerId);
        Long currentUserId = getCurrentUserId();
        UserDTO requester = userServiceClient.getUserById(currentUserId);
        UserDTO manager = userServiceClient.getUserById(managerId);
        if (manager == null) {
            throw new UserNotFoundException("Manager not found with ID: " + managerId);
        }
        if ((!currentUserId.equals(managerId)
                || !requester.getRole().equals(Role.MANAGER))
                && !requester.getRole().equals(Role.ADMIN)) {

            System.out.println(requester.getRole());
            log.warn("Unauthorized access attempt to view recognitions for user {} by user {}", managerId, currentUserId);
            throw new UnauthorizedAccessException("You can only view recognitions received by your own account.");
        }

        // Get team members reporting to this manager via User Service
        List<UserDTO> teamMembers = userServiceClient.getTeamMembersByManagerId(managerId);
        List<Long> teamUserIds = teamMembers.stream()
                .map(UserDTO::getUserId)
                .collect(Collectors.toList());

        if (teamUserIds.isEmpty()) {
            return List.of();
        }

        return feedbackRepository.findAllByTargetUserIdIn(teamUserIds);
    }

    @Transactional
    public Feedback toggleVisibility(Long id, Long currentUserId) {
        Feedback feedback = getFeedbackById(id);

        if (!feedback.getSenderId().equals(currentUserId)) {
            throw new UnauthorizedAccessException("Unauthorized visibility change.");
        }

        feedback.setAnonymous(!feedback.isAnonymous());
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getFeedbacksSentBy(Long senderId) {
        log.info("Fetching feedback sent by user: {}", senderId);
        return feedbackRepository.findAllBySenderIdOrderBySubmittedDateDesc(senderId);
    }

    public List<Feedback> getFeedbacksReceivedBy(Long targetUserId) {
        log.info("Fetching feedback received by user: {}", targetUserId);
        return feedbackRepository.findAllByTargetUserIdOrderBySubmittedDateDesc(targetUserId);
    }

    public long countByTargetUserId(Long targetUserId) {
        UserDTO targetUser = userServiceClient.getUserById(targetUserId);
        if (targetUser == null) {
            throw new UserNotFoundException("Target user not found with ID: " + targetUserId);
        }
        Long currentUserId = getCurrentUserId();
        UserDTO currentUser = userServiceClient.getUserById(currentUserId);
        if(!currentUserId.equals(targetUserId) && !currentUser.getRole().equals(Role.ADMIN) && !targetUser.getManagerId().toString().equals(currentUserId.toString())) {
            log.warn("Unauthorized access attempt to count feedback for user {} by user {}", targetUserId, currentUserId);
            throw new UnauthorizedAccessException("You can only count feedback received by your own account.");
        }
        return feedbackRepository.countByTargetUserId(targetUserId);
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

    public List<Feedback> getAll(Long currentUserId) {
        UserDTO requester = userServiceClient.getUserById(currentUserId);
        if(!requester.getRole().equals(Role.ADMIN)){
            throw new UnauthorizedAccessException("Only admin can view all the feedbacks");
        }
        return feedbackRepository.findAll();
    }
}