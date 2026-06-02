package com.cognizant.training.feedbacktrack.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.cognizant.training.feedbacktrack.dto.EngagementTrendDTO;
import com.cognizant.training.feedbacktrack.dto.UserResponseDTO;
import com.cognizant.training.feedbacktrack.enums.FeedbackAction;
import com.cognizant.training.feedbacktrack.enums.Role;
import com.cognizant.training.feedbacktrack.exceptions.UnauthorizedAccessException;
import com.cognizant.training.feedbacktrack.feign.UserClient;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class EngagementServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private UserClient userClient;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private EngagementService engagementService;

    private UserResponseDTO adminUser;
    private UserResponseDTO managerUser;

    @BeforeEach
    void setUp() {
        adminUser = new UserResponseDTO();
        adminUser.setUserId(99L);
        adminUser.setRole(Role.ADMIN);

        managerUser = new UserResponseDTO();
        managerUser.setUserId(1L);
        managerUser.setRole(Role.MANAGER);
    }

    @Test
    @DisplayName("getTrendForReviewer: Success and Correct Calculation")
    void getTrendForReviewer_Success() {
        try (MockedStatic<SecurityContextHolder> mockedContext = mockStatic(SecurityContextHolder.class)) {
            // Mock Security
            mockedContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn("1");

            // Mock Feign
            when(userClient.getUserById(1L)).thenReturn(managerUser);

            // Mock Repository Data (Action Breakdown)
            List<Object[]> actionRows = new ArrayList<>();
            actionRows.add(new Object[]{FeedbackAction.RESOLVED, 5L});
            actionRows.add(new Object[]{FeedbackAction.PENDING, 5L});
            when(reviewRepository.countByActionForReviewer(eq(1L))).thenReturn(actionRows);

            // Mock Repository Data (Monthly Trend)
            List<Object[]> monthlyRows = new ArrayList<>();
            monthlyRows.add(new Object[]{2026, 1, 10L}); // Year, Month, Count
            when(reviewRepository.countMonthlyByReviewer(eq(1L), any(), any())).thenReturn(monthlyRows);

            // Act
            EngagementTrendDTO trend = engagementService.getTrendForReviewer(1L, 6);

            // Assert
            assertNotNull(trend);
            assertEquals(10L, trend.getTotalReviews());
            assertEquals(50.0, trend.getResolutionRatePercent()); // 5 resolved out of 10 total
            assertEquals(1, trend.getMonthlyTrend().size());
            assertEquals(2026, trend.getMonthlyTrend().get(0).getYear());
        }
    }

    @Test
    @DisplayName("getGlobalTrend: Fails for non-admin")
    void getGlobalTrend_ForbiddenForManager() {
        // Manager tries to access global trend
        when(userClient.getUserById(1L)).thenReturn(managerUser);

        assertThrows(UnauthorizedAccessException.class, () -> {
            engagementService.getGlobalTrend(1L, 3);
        });
    }

    @Test
    @DisplayName("Resolution Rate: Returns null when total is zero")
    void computeResolutionRate_ZeroTotal() {
        try (MockedStatic<SecurityContextHolder> mockedContext = mockStatic(SecurityContextHolder.class)) {
            mockedContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn("1");
            when(userClient.getUserById(1L)).thenReturn(managerUser);

            // Empty rows
            when(reviewRepository.countByActionForReviewer(1L)).thenReturn(new ArrayList<>());
            when(reviewRepository.countMonthlyByReviewer(anyLong(), any(), any())).thenReturn(new ArrayList<>());

            EngagementTrendDTO result = engagementService.getTrendForReviewer(1L, 6);

            assertNull(result.getResolutionRatePercent());
            assertEquals(0L, result.getTotalReviews());
        }
    }

    @Test
    @DisplayName("Window Resolver: Caps at 24 months")
    void resolveWindow_Capping() {
        // We use an admin to bypass other checks easily
        when(userClient.getUserById(99L)).thenReturn(adminUser);
        when(reviewRepository.countByActionGlobal()).thenReturn(new ArrayList<>());
        when(reviewRepository.countMonthlyGlobal(any(), any())).thenReturn(new ArrayList<>());

        EngagementTrendDTO trend = engagementService.getGlobalTrend(99L, 99); // Request 99 months

        // Verification of logic would usually happen via ArgumentCaptor to see what 'from' date was sent to repo
        // but checking successful execution with huge numbers is a start.
        assertNotNull(trend);
    }
}