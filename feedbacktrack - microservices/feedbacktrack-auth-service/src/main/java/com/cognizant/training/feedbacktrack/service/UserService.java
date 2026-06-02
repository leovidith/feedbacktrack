package com.cognizant.training.feedbacktrack.service;

import java.util.List;

import com.cognizant.training.feedbacktrack.dto.CreateUserRequest;
import com.cognizant.training.feedbacktrack.dto.UserResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cognizant.training.feedbacktrack.model.User;
import com.cognizant.training.feedbacktrack.model.Department;
import com.cognizant.training.feedbacktrack.repository.UserRepository;
import com.cognizant.training.feedbacktrack.enums.Role;
import com.cognizant.training.feedbacktrack.exception.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired DepartmentService departmentService;

    /**
     * Registers a new employee from a {@link CreateUserRequest} DTO.
     * Encodes the password, resolves the department via the Department Service,
     * and links the manager from the local User DB.
     */
    public User registerEmployee(CreateUserRequest request) {
        log.info("Attempting to register employee with email: {}", request.getEmail());

        // Check for duplicate email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.error("Registration failed: email {} is already in use", request.getEmail());
            throw new RuntimeException("A user with email '" + request.getEmail() + "' already exists.");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus(request.getStatus() != null ? request.getStatus() : "Active");

        log.debug("Password encoded successfully for user: {}", request.getEmail());

        // INTER-SERVICE CALL: Verify department exists in Department Microservice
        if (request.getDepartmentId() != null) {
            log.debug("Fetching department details from remote service for ID: {}", request.getDepartmentId());
            try {
                Department dept = departmentService.getDepartmentById(request.getDepartmentId());
                user.setDepartment(dept);
                log.info("Linked user to remote department: {}", dept.getDepartmentName());
            } catch (Exception e) {
                log.error("Registration failed: Department Service unavailable or ID {} not found",
                          request.getDepartmentId());
                throw new ResourceNotFoundException("Department ID " + request.getDepartmentId() + " not valid.");
            }
        }

        // Internal Hierarchy: Check manager existence in local User DB
        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Manager ID " + request.getManagerId() + " not found in DB"));
            user.setManager(manager);
            log.info("Linked user to manager: {}", manager.getName());
        }

        User saved = userRepository.save(user);
        log.info("Successfully saved user: {} with ID: {}", saved.getName(), saved.getUserId());
        return saved;
    }
    
    public User getUserByEmail(String email) {
        log.debug("Fetching user profile for email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User retrieval failed: Email {} not found", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });
    }

    public User getUserById(Long id) {
        log.debug("Fetching user profile for ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User retrieval failed: ID {} not found", id);
                    return new ResourceNotFoundException("User not found with ID: " + id);
                });
        return user;
    }

    public List<User> getTeamMembersByManagerId(Long managerId) {
        log.debug("Fetching team members for manager ID: {}", managerId);
        return userRepository.findByManagerUserId(managerId);

    }

    public User updateRole(String email, Role role) {
        log.info("Updating role for user {} to {}", email, role);
        User user = getUserByEmail(email);
        user.setRole(role);
        if (user.getStatus() == null) { user.setStatus("Active"); }
        return userRepository.save(user); 
    }
    
    public User deactivateUser(String email, String currentUserEmail) {
        log.warn("Processing deactivation request for user account: {}", email);
        User targetUser = getUserByEmail(email);

        // Security logic for FeedbackTrack
        if (email.equalsIgnoreCase(currentUserEmail)) {
            throw new RuntimeException("Security Violation: You cannot deactivate your own account.");
        }

        if (Role.ADMIN.equals(targetUser.getRole())) {
            throw new RuntimeException("Hierarchy Violation: Admin accounts cannot be deactivated.");
        }

        targetUser.setStatus("Inactive");
        return userRepository.save(targetUser); 
    }
    
    public User activateUser(String email) {
        log.info("Processing re-activation request for user account: {}", email);
        User user = getUserByEmail(email);

        if (Role.ADMIN.equals(user.getRole())) {
            throw new RuntimeException("Hierarchy Violation: Admin accounts cannot be modified status-wise.");
        }

        user.setStatus("Active");
        return userRepository.save(user);
    }
   
    public boolean changePassword(String email, String oldPassword, String newPassword) {
        log.info("Password change request for user: {}", email);
        User user = getUserByEmail(email);
        
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            log.info("Password updated successfully for user: {}", email);
            return true;
        }
        return false;
    }
    
    public List<User> getAllEmployees() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }


}