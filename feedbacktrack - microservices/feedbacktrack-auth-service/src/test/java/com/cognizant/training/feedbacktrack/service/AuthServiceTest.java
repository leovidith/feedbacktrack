package com.cognizant.training.feedbacktrack.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cognizant.training.feedbacktrack.model.User;
import com.cognizant.training.feedbacktrack.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void testValidateUser_WrongPassword() {
        User mockUser = new User();
        mockUser.setPassword("hashedPass");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongPass", "hashedPass")).thenReturn(false);

        // Should throw RuntimeException as per your AuthService code
        assertThrows(RuntimeException.class, () -> authService.validateUser("test@test.com", "wrongPass"));
    }
}