package com.cognizant.training.feedbacktrack.service;

import com.cognizant.training.feedbacktrack.dto.BadgeDTO;
import com.cognizant.training.feedbacktrack.dto.CreateRecognitionRequest;
import com.cognizant.training.feedbacktrack.dto.UserDTO;
import com.cognizant.training.feedbacktrack.enums.Role;
import com.cognizant.training.feedbacktrack.exceptions.InvalidRecognitionException;
import com.cognizant.training.feedbacktrack.exceptions.RecognitionNotFoundException;
import com.cognizant.training.feedbacktrack.exceptions.UnauthorizedAccessException;
import com.cognizant.training.feedbacktrack.feign.AdminServiceClient;
import com.cognizant.training.feedbacktrack.feign.UserServiceClient;
import com.cognizant.training.feedbacktrack.model.Recognition;
import com.cognizant.training.feedbacktrack.repository.RecognitionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecognitionServiceTest {

    @Mock
    private RecognitionRepository recognitionRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private AdminServiceClient adminServiceClient;

    @InjectMocks
    private RecognitionService recognitionService;

    // Standardized test variables
    private Recognition testRecognition;
    private CreateRecognitionRequest validRequest;
    private final Long SENDER_ID = 101L;
    private final Long TARGET_ID = 102L;
    private final Long BADGE_ID = 5L;
    private final Long RECOGNITION_ID = 1L;

    @BeforeEach
    void setUp() {
        setAuthenticatedUser(SENDER_ID);

        validRequest = new CreateRecognitionRequest();
        validRequest.setTargetUserId(TARGET_ID);
        validRequest.setBadgeId(BADGE_ID);
        validRequest.setMessage("Great job on the project!");

        testRecognition = new Recognition();
        testRecognition.setRecognitionId(RECOGNITION_ID);
        testRecognition.setSenderId(SENDER_ID);
        testRecognition.setTargetUserId(TARGET_ID);
        testRecognition.setBadgeId(BADGE_ID);
        testRecognition.setMessage("Great job on the project!");
        testRecognition.setRecognizedDate(LocalDateTime.now());

        lenient().when(userServiceClient.getUserById(SENDER_ID)).thenReturn(buildUser(SENDER_ID, Role.EMPLOYEE));
        lenient().when(userServiceClient.getUserById(TARGET_ID)).thenReturn(buildUser(TARGET_ID, Role.EMPLOYEE));
        lenient().when(adminServiceClient.getBadgeById(BADGE_ID)).thenReturn(new BadgeDTO(BADGE_ID, "Kudos", 10, "desc", "icon"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- Tests for create() ---

    @Test
    @DisplayName("Should successfully create a recognition when request is valid")
    void testCreate_Success() {
        when(recognitionRepository.save(any(Recognition.class))).thenAnswer(invocation -> {
            Recognition saved = invocation.getArgument(0);
            saved.setRecognitionId(RECOGNITION_ID);
            return saved;
        });

        Recognition result = recognitionService.create(validRequest);

        assertNotNull(result, "Resulting recognition should not be null");
        assertEquals(RECOGNITION_ID, result.getRecognitionId());
        assertEquals(SENDER_ID, result.getSenderId());
        assertEquals(TARGET_ID, result.getTargetUserId());
        assertEquals("Great job on the project!", result.getMessage());

        verify(recognitionRepository, times(1)).save(any(Recognition.class));
    }

    @Test
    @DisplayName("Should throw InvalidRecognitionException when user tries to recognize themselves")
    void testCreate_SelfRecognition_ThrowsException() {
        validRequest.setTargetUserId(SENDER_ID);
        InvalidRecognitionException exception = assertThrows(
                InvalidRecognitionException.class,
                () -> recognitionService.create(validRequest)
        );

        assertEquals("Self-recognition is not permitted.", exception.getMessage());
        verify(recognitionRepository, never()).save(any(Recognition.class));
    }

    // --- Tests for findReceived() and findSent() ---

    @Test
    @DisplayName("Should return a list of recognitions received by current user")
    void testFindReceived() {
        List<Recognition> expectedList = Arrays.asList(testRecognition);
        when(recognitionRepository.findAllByTargetUserId(SENDER_ID)).thenReturn(expectedList);

        List<Recognition> result = recognitionService.findReceived(SENDER_ID);

        assertEquals(1, result.size());
        verify(recognitionRepository, times(1)).findAllByTargetUserId(SENDER_ID);
    }

    @Test
    @DisplayName("Should return a list of recognitions sent by current user")
    void testFindSent() {
        List<Recognition> expectedList = Arrays.asList(testRecognition);
        when(recognitionRepository.findAllBySenderId(SENDER_ID)).thenReturn(expectedList);

        List<Recognition> result = recognitionService.findSent(SENDER_ID);

        assertEquals(1, result.size());
        assertEquals(SENDER_ID, result.get(0).getSenderId());
        verify(recognitionRepository, times(1)).findAllBySenderId(SENDER_ID);
    }

    // --- Tests for delete() ---

    @Test
    @DisplayName("Should delete recognition when current user is the owner")
    void testDelete_Success() {
        when(recognitionRepository.findById(RECOGNITION_ID)).thenReturn(Optional.of(testRecognition));

        recognitionService.delete(RECOGNITION_ID);

        verify(recognitionRepository, times(1)).findById(RECOGNITION_ID);
        verify(recognitionRepository, times(1)).delete(testRecognition);
    }

    @Test
    @DisplayName("Should throw RecognitionNotFoundException when attempting to delete a non-existent ID")
    void testDelete_NotFound_ThrowsException() {
        when(recognitionRepository.findById(RECOGNITION_ID)).thenReturn(Optional.empty());

        assertThrows(
                RecognitionNotFoundException.class,
                () -> recognitionService.delete(RECOGNITION_ID)
        );

        verify(recognitionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedAccessException when non-owner tries to delete")
    void testDelete_Unauthorized_ThrowsException() {
        Long unauthorizedUserId = 999L;
        setAuthenticatedUser(unauthorizedUserId);
        when(recognitionRepository.findById(RECOGNITION_ID)).thenReturn(Optional.of(testRecognition));
        when(userServiceClient.getUserById(unauthorizedUserId)).thenReturn(buildUser(unauthorizedUserId, Role.EMPLOYEE));

        UnauthorizedAccessException exception = assertThrows(
                UnauthorizedAccessException.class,
                () -> recognitionService.delete(RECOGNITION_ID)
        );

        assertEquals("You lack permissions to delete this recognition.", exception.getMessage());
        verify(recognitionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should allow ADMIN to delete someone else's recognition")
    void testDelete_AdminCanDelete() {
        Long adminId = 500L;
        setAuthenticatedUser(adminId);
        when(recognitionRepository.findById(RECOGNITION_ID)).thenReturn(Optional.of(testRecognition));
        when(userServiceClient.getUserById(adminId)).thenReturn(buildUser(adminId, Role.ADMIN));

        recognitionService.delete(RECOGNITION_ID);

        verify(recognitionRepository, times(1)).delete(testRecognition);
    }

    // --- Test for getTeamRecognitions() ---

    @Test
    @DisplayName("Should throw UnauthorizedAccessException when user accesses another manager's team recognitions")
    void testGetTeamRecognitions_ThrowsException() {
        assertThrows(
                UnauthorizedAccessException.class,
                () -> recognitionService.getTeamRecognitions(TARGET_ID)
        );
    }

    // --- Tests for viewRecognition() and viewAll() ---

    @Test
    @DisplayName("Should return a specific recognition when it exists")
    void testViewRecognition_Success() {
        // Arrange
        when(recognitionRepository.findById(RECOGNITION_ID)).thenReturn(Optional.of(testRecognition));

        // Act
        Recognition result = recognitionService.viewRecognition(RECOGNITION_ID);

        // Assert
        assertNotNull(result);
        assertEquals(RECOGNITION_ID, result.getRecognitionId());
        verify(recognitionRepository, times(1)).findById(RECOGNITION_ID);
    }

    @Test
    @DisplayName("Should throw RecognitionNotFoundException when viewing a non-existent recognition")
    void testViewRecognition_NotFound_ThrowsException() {
        // Arrange
        when(recognitionRepository.findById(RECOGNITION_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                RecognitionNotFoundException.class,
                () -> recognitionService.viewRecognition(RECOGNITION_ID)
        );
    }

    @Test
    @DisplayName("Should return all recognitions")
    void testViewAll() {
        Long managerId = 201L;
        setAuthenticatedUser(managerId);
        when(userServiceClient.getUserById(managerId)).thenReturn(buildUser(managerId, Role.MANAGER));
        List<Recognition> expectedList = Arrays.asList(testRecognition, new Recognition());
        when(recognitionRepository.findAll()).thenReturn(expectedList);

        List<Recognition> result = recognitionService.viewAll();

        assertEquals(2, result.size());
        verify(recognitionRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should throw UnauthorizedAccessException when employee tries to view all recognitions")
    void testViewAll_Unauthorized() {
        when(userServiceClient.getUserById(SENDER_ID)).thenReturn(buildUser(SENDER_ID, Role.EMPLOYEE));

        assertThrows(UnauthorizedAccessException.class, () -> recognitionService.viewAll());
        verify(recognitionRepository, never()).findAll();
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList())
        );
    }

    private UserDTO buildUser(Long id, Role role) {
        UserDTO user = new UserDTO();
        user.setUserId(id);
        user.setRole(role);
        return user;
    }
}