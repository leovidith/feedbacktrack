package com.cognizant.training.feedbacktrack.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cognizant.training.feedbacktrack.model.User;
import com.cognizant.training.feedbacktrack.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User validateUser(String email, String rawPassword) {
        log.info("Processing login request for email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed: Account with email {} does not exist", email);
                    return new RuntimeException("User not found");
                });
        if ("Inactive".equalsIgnoreCase(user.getStatus())) {
            log.warn("Login blocked: Account {} is currently de-activated", email);
            throw new RuntimeException("Account is inactive. Please contact your manager.");
        }

        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.info("User {} authenticated successfully with role: {}", email, user.getRole());
            return user; 
        } else {
            log.error("Authentication failed: Incorrect password provided for user {}", email);
            throw new RuntimeException("Invalid password"); 
        }
    }
}