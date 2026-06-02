package com.cognizant.training.feedbacktrack.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.cognizant.training.feedbacktrack.model.BadgeLibrary;
import com.cognizant.training.feedbacktrack.repository.BadgeLibraryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;



@ExtendWith(MockitoExtension.class)
public class BadgeLibraryServiceTest {

    @Mock
    private BadgeLibraryRepository badgeRepo; 

    @InjectMocks
    private BadgeLibraryService badgeService; 

    @Test
    void shouldSaveBadgeSuccessfully() {
        
        BadgeLibrary badge = new BadgeLibrary(1L, "Team Player", 20, "Helpful", "icon.png");
        when(badgeRepo.existsByBadgeName("Team Player")).thenReturn(false);
        when(badgeRepo.save(any(BadgeLibrary.class))).thenReturn(badge);

        
        BadgeLibrary savedBadge = badgeService.addBadge(badge);

        
        assertNotNull(savedBadge);
        assertEquals("Team Player", savedBadge.getBadgeName());
        verify(badgeRepo, times(1)).save(badge); 
    }

    @Test
    void shouldThrowExceptionWhenBadgeNameExists() {
 
        BadgeLibrary badge = new BadgeLibrary(null, "Innovator", 50, "Creative", "icon.png");
        when(badgeRepo.existsByBadgeName("Innovator")).thenReturn(true);

        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            badgeService.addBadge(badge);
        });

        assertEquals("Badge name 'Innovator' already exists in the library.", exception.getMessage());
        verify(badgeRepo, never()).save(any()); 
    }
}