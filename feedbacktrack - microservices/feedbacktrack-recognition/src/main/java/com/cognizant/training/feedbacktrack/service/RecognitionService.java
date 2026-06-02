package com.cognizant.training.feedbacktrack.service;

import com.cognizant.training.feedbacktrack.dto.*;
import com.cognizant.training.feedbacktrack.enums.Role;
import com.cognizant.training.feedbacktrack.exceptions.*;
import com.cognizant.training.feedbacktrack.feign.AdminServiceClient;
import com.cognizant.training.feedbacktrack.feign.UserServiceClient;
import com.cognizant.training.feedbacktrack.model.Recognition;
import com.cognizant.training.feedbacktrack.repository.RecognitionRepository;
import jakarta.transaction.Transactional;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RecognitionService {

    @Autowired
    private RecognitionRepository recognitionRepository;

    @Autowired
    private UserServiceClient userServiceClient;       // ← NEW

    @Autowired
    private AdminServiceClient adminServiceClient;

    @Transactional
    public Recognition create(CreateRecognitionRequest request) {
        Long currentUserId = getCurrentUserId();
        log.info("Processing creation of new recognition from {} to {}", currentUserId, request.getTargetUserId());

        // Logical Validation: Cannot recognize self
        if (currentUserId.equals(request.getTargetUserId())) {
            throw new InvalidRecognitionException("Self-recognition is not permitted.");
        }

        UserDTO sender = userServiceClient.getUserById(currentUserId);
        UserDTO target = userServiceClient.getUserById(request.getTargetUserId());
        BadgeDTO badge = adminServiceClient.getBadgeById(request.getBadgeId());

        Recognition recognition = new Recognition();
        recognition.setSenderId(currentUserId);     // Store ID only
        recognition.setTargetUserId(request.getTargetUserId()); // Store ID only
        recognition.setBadgeId(request.getBadgeId());       // Store ID only
        recognition.setMessage(request.getMessage());
        recognition.setRecognizedDate(LocalDateTime.now());

        Recognition saved = recognitionRepository.save(recognition);
        log.info("Successfully saved Recognition ID: {}", saved.getRecognitionId());
        return saved;
    }

    public List<Recognition> findReceived(Long userId) {
        log.debug("Fetching recognitions received by user: {}", userId);
        UserDTO user=userServiceClient.getUserById(userId);
        Long currentUserId = getCurrentUserId();
        UserDTO requester = userServiceClient.getUserById(currentUserId);
        if (!currentUserId.equals(userId) && !requester.getRole().equals(Role.ADMIN) && !(user.getManagerId() != null && user.getManagerId().equals(currentUserId))) {
            log.warn("Unauthorized access attempt to view recognitions for user {} by user {}", userId, currentUserId);
            throw new UnauthorizedAccessException("You can only view recognitions received by your own account.");
        }
        return recognitionRepository.findAllByTargetUserId(userId);
    }

    public List<Recognition> findSent(Long userId) {
        log.debug("Fetching recognitions sent by user: {}", userId);
        UserDTO user=userServiceClient.getUserById(userId);
        Long currentUserId = getCurrentUserId();
        UserDTO requester = userServiceClient.getUserById(currentUserId);
        if (!currentUserId.equals(userId) && !requester.getRole().equals(Role.ADMIN) && !user.getManagerId().equals(currentUserId)) {
            log.warn("Unauthorized access attempt to view recognitions for user {} by user {}", userId, currentUserId);
            throw new UnauthorizedAccessException("You can only view recognitions received by your own account.");
        }
        return recognitionRepository.findAllBySenderId(userId);
    }

    @Transactional
    public void delete(Long recognitionId) {
        Long currentUserId = getCurrentUserId();
        log.info("User {} attempting to delete recognition {}", currentUserId, recognitionId);

        Recognition recognition = recognitionRepository.findById(recognitionId)
                .orElseThrow(() -> new RecognitionNotFoundException("Recognition record not found"));

        // Step 1: Check ownership locally
        boolean isOwner = recognition.getSenderId().equals(currentUserId);

        UserDTO user = userServiceClient.getUserById(currentUserId);
        boolean isAdmin = user != null && user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            log.warn("Unauthorized delete attempt by user {}", currentUserId);
            throw new UnauthorizedAccessException("You lack permissions to delete this recognition.");
        }

        recognitionRepository.delete(recognition);
        log.info("User {} successfully deleted recognition {}", currentUserId, recognitionId);
    }

    public List<Recognition> getTeamRecognitions(Long managerId) {
        log.info("Retrieving team recognition history for manager: {}", managerId);
        Long currentUserId = getCurrentUserId();
        UserDTO requester = userServiceClient.getUserById(currentUserId);
        if (!currentUserId.equals(managerId) && !requester.getRole().equals(Role.ADMIN)) {
            log.warn("Unauthorized access attempt to view recognitions for user {} by user {}", managerId, currentUserId);
            throw new UnauthorizedAccessException("You can only view recognitions received by your own account.");
        }
        // Step 1: Get all team members under this manager from the User Service
        List<UserDTO> teamMembers = userServiceClient.getTeamMembersByManagerId(managerId);
        log.debug("Manager {} has {} team members", managerId, teamMembers.size());

        // Step 2: Extract userIds from the UserDTOs
        List<Long> teamUserIds = teamMembers.stream()
                .map(UserDTO::getUserId)
                .toList();

        // Step 3: Fetch recognitions received by any of those users
        return recognitionRepository.findAllByTargetUserIdIn(teamUserIds);
    }

    public Recognition viewRecognition(Long id) {
        log.debug("Fetching recognition by id: {}", id);
        Long currentUserId = getCurrentUserId();
        UserDTO requester = userServiceClient.getUserById(currentUserId);
        Recognition recognition = recognitionRepository.findById(id)
                .orElseThrow(() -> new RecognitionNotFoundException("Recognition ID " + id + " not found."));
        if (!recognition.getSenderId().equals(currentUserId) && !recognition.getTargetUserId().equals(currentUserId) && (requester == null || requester.getRole() != Role.ADMIN)) {
            log.warn("Unauthorized access attempt to view recognition {} by user {}", id, currentUserId);
            throw new UnauthorizedAccessException("You can only view recognitions sent or received by your own account.");
        }
        log.debug("Recognition {} fetched successfully", id);
        return recognition;
    }

    public List<Recognition> viewAll() {
        Long currentUserId = getCurrentUserId();
        log.debug("Fetching all recognitions of all users of all time");

        // Fetch the current user to check their role
        UserDTO user = userServiceClient.getUserById(currentUserId);

        // Only ADMIN and MANAGER can view all recognitions feed
        if (user == null || (user.getRole() != Role.ADMIN) ) {
            log.warn("Unauthorized attempt to view all recognitions by non-admin/manager user: {}", currentUserId);
            throw new UnauthorizedAccessException("Only Admin or Manager can view all recognitions.");
        }

        List<Recognition> recognitions = recognitionRepository.findAll();
        log.debug("Fetched {} recognition records", recognitions.size());
        return recognitions;
    }

    public Integer sumPointsByTargetUserId(Long targetUserId) {
        log.info("Calculating total recognition points for user: {}", targetUserId);

        // Verify that the user exists in the User Service
        UserDTO requester = userServiceClient.getUserById(getCurrentUserId());
        UserDTO user = userServiceClient.getUserById(targetUserId);
        log.debug("User {} verified in User Service", targetUserId);

        // Step 1: Fetch all recognitions received by this user
        if(!requester.getUserId().equals(targetUserId) && requester.getRole() != Role.ADMIN && !user.getManagerId().equals(requester.getUserId().toString())) {
            log.warn("Unauthorized access attempt to view recognitions for user {} by user {}", targetUserId, requester.getUserId());
            throw new UnauthorizedAccessException("You can only view recognitions received by your own account.");
        }
        List<Recognition> recognitions = recognitionRepository.findAllByTargetUserId(targetUserId);
        log.debug("Found {} recognitions for user {}", recognitions.size(), targetUserId);

        // Step 2: Sum up the points from all badges
        Integer totalPoints = 0;
        for (Recognition recognition : recognitions) {
            BadgeDTO badge = adminServiceClient.getBadgeById(recognition.getBadgeId());
            if (badge != null && badge.getPointsValue() != null) {
                totalPoints += badge.getPointsValue();
                log.debug("Badge {} has {} points", badge.getBadgeId(), badge.getPointsValue());
            }
        }

        log.info("User {} has accumulated {} total points from {} recognitions", 
                 targetUserId, totalPoints, recognitions.size());
        return totalPoints;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("User is not authenticated.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long currentUserId) {
            return currentUserId;
        }
        if (principal instanceof String principalText && !"anonymousUser".equalsIgnoreCase(principalText)) {
            try {
                return Long.parseLong(principalText);
            } catch (NumberFormatException ex) {
                throw new UnauthorizedAccessException("Invalid authenticated user id.");
            }
        }

        throw new UnauthorizedAccessException("Unable to resolve authenticated user id.");
    }
}
