package com.cognizant.training.feedbacktrack.service;

import com.cognizant.training.feedbacktrack.dto.CreateFeedbackRequest;
import com.cognizant.training.feedbacktrack.dto.FeedbackCategoryDTO;
import com.cognizant.training.feedbacktrack.dto.UserDTO;
import com.cognizant.training.feedbacktrack.enums.Role;
import com.cognizant.training.feedbacktrack.exceptions.InvalidFeedbackException;
import com.cognizant.training.feedbacktrack.model.Feedback;
import com.cognizant.training.feedbacktrack.repository.FeedbackRepository;
import com.cognizant.training.feedbacktrack.feign.FeedbackCategoryClient;
import com.cognizant.training.feedbacktrack.feign.UserServiceClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private FeedbackCategoryClient feedbackCategoryClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private FeedbackService feedbackService;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createNewFeedback_Success() {
        // Arrange
        CreateFeedbackRequest request = new CreateFeedbackRequest();
        request.setSenderId(1L);
        request.setTargetUserId(2L);
        request.setCategoryId(10L);
        request.setComments("Great collaboration on the last sprint!");
        request.setAnonymous(false);

        UserDTO sender = mock(UserDTO.class);
        UserDTO targetUser = mock(UserDTO.class);
        FeedbackCategoryDTO category = mock(FeedbackCategoryDTO.class);

        Feedback mockSavedFeedback = new Feedback();
        mockSavedFeedback.setFeedbackId(100L);
        mockSavedFeedback.setSenderId(1L);
        mockSavedFeedback.setTargetUserId(2L);
        mockSavedFeedback.setCategoryId(10L);
        mockSavedFeedback.setComments("Great collaboration on the last sprint!");

        when(userServiceClient.getUserById(1L)).thenReturn(sender);
        when(userServiceClient.getUserById(2L)).thenReturn(targetUser);
        when(feedbackCategoryClient.getCategoryById(10L)).thenReturn(category);
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(mockSavedFeedback);

        // Act
        Feedback result = feedbackService.createNewFeedback(request);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getFeedbackId());
        assertEquals(1L, result.getSenderId());
        assertEquals(2L, result.getTargetUserId());
        verify(feedbackRepository, times(1)).save(any(Feedback.class));
    }

    @Test
    void createNewFeedback_ThrowsInvalidFeedbackException_WhenSenderIsTarget() {
        // Arrange
        CreateFeedbackRequest request = new CreateFeedbackRequest();
        request.setSenderId(1L);
        request.setTargetUserId(1L); // Self feedback

        // Act & Assert
        assertThrows(InvalidFeedbackException.class, () -> feedbackService.createNewFeedback(request));

        // Ensure no external calls or saves happened
        verifyNoInteractions(userServiceClient, feedbackCategoryClient, feedbackRepository);
    }


    @Test
    void getFeedbacksSentBy_Success() {
        // Arrange
        Long senderId = 1L;
        Feedback f1 = new Feedback();
        f1.setFeedbackId(201L);
        f1.setSenderId(senderId);

        Feedback f2 = new Feedback();
        f2.setFeedbackId(202L);
        f2.setSenderId(senderId);

        when(feedbackRepository.findAllBySenderIdOrderBySubmittedDateDesc(senderId))
                .thenReturn(List.of(f1, f2));

        // Act
        List<Feedback> results = feedbackService.getFeedbacksSentBy(senderId);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(201L, results.get(0).getFeedbackId());
        verify(feedbackRepository, times(1)).findAllBySenderIdOrderBySubmittedDateDesc(senderId);
    }

    @Test
    void getFeedbackById_Success_WhenUserIsAdmin() {
        // Arrange
        Long feedbackId = 500L;
        Long currentAdminId = 99L;

        // Mocking Security Principal extraction
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(currentAdminId);

        UserDTO adminUser = mock(UserDTO.class);
        when(adminUser.getRole()).thenReturn(Role.ADMIN);
        when(userServiceClient.getUserById(currentAdminId)).thenReturn(adminUser);

        Feedback feedback = new Feedback();
        feedback.setFeedbackId(feedbackId);
        feedback.setSenderId(1L);
        feedback.setTargetUserId(2L); // Admin is neither sender nor recipient

        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedback));

        // Act
        Feedback result = feedbackService.getFeedbackById(feedbackId);

        // Assert
        assertNotNull(result);
        assertEquals(feedbackId, result.getFeedbackId());
        verify(feedbackRepository, times(1)).findById(feedbackId);
    }
}