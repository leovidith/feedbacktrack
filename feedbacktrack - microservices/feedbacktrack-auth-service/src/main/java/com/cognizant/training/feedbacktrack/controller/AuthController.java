package com.cognizant.training.feedbacktrack.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cognizant.training.feedbacktrack.config.JwtUtil;
import com.cognizant.training.feedbacktrack.model.User;
import com.cognizant.training.feedbacktrack.service.AuthService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/auth/")
// @CrossOrigin("*") 
@Slf4j 
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        log.info("REST request to login for user: {}", email); 

        
        User user = authService.validateUser(email, password);

        log.info("User {} successfully validated. Generating JWT token...", email);
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole().name());

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole().name());
        response.put("name", user.getName());
        
        log.info("Login successful for user: {}. Role: {}", user.getName(), user.getRole());
        
        return response;
    }
}