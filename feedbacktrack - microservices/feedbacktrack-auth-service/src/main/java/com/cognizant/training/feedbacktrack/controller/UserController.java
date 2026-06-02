package com.cognizant.training.feedbacktrack.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.cognizant.training.feedbacktrack.model.User;
import com.cognizant.training.feedbacktrack.dto.CreateUserRequest;
import com.cognizant.training.feedbacktrack.dto.UserResponseDTO;
import com.cognizant.training.feedbacktrack.service.UserService;

import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
// @CrossOrigin("*") 
@Slf4j 
public class UserController {

    @Autowired
    private UserService userService;

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/add")
    public ResponseEntity<UserResponseDTO> addEmployee(@Valid @RequestBody CreateUserRequest request) {
        log.info("REST request to add new employee: {}", request.getEmail());
        User savedUser = userService.registerEmployee(request);
        log.info("Successfully onboarded employee with ID: {}", savedUser.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponseDTO(savedUser));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponseDTO> getProfile(@PathVariable String email) {
        log.info("REST request to get profile for email: {}", email);
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(mapToResponseDTO(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        log.info("REST request to get user by ID: {}", id);
        User user = userService.getUserById(id);
        return ResponseEntity.ok(mapToResponseDTO(user));
    }

    @GetMapping("/manager/{managerId}/team")
    public ResponseEntity<List<UserResponseDTO>> getTeamMembersByManagerId(@PathVariable Long managerId) {
        log.info("REST request to get team members for manager ID: {}", managerId);
        List<UserResponseDTO> teamMembers = userService.getTeamMembersByManagerId(managerId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        log.debug("Returning {} team members for manager ID: {}", teamMembers.size(), managerId);
        return ResponseEntity.ok(teamMembers);
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDTO>> getAll() {
        log.info("REST request to fetch all users");
        List<UserResponseDTO> dtoList = userService.getAllEmployees()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        log.debug("Returning {} users in the list", dtoList.size());
        return ResponseEntity.ok(dtoList);
    }
 
    @PreAuthorize("hasRole('ADMIN')") // Only ADMIN can change someone's rank
    @PutMapping("/{email}/role")
    public ResponseEntity<UserResponseDTO> updateRole(@PathVariable String email, @RequestParam com.cognizant.training.feedbacktrack.enums.Role role) {
        log.warn("ADMIN ACTION: Request to update role for {} to {}", email, role);
        
        // We call updateRole, NOT registerEmployee
        User updatedUser = userService.updateRole(email, role); 
        
        log.info("Role updated successfully for user: {}", email);
        return ResponseEntity.ok(mapToResponseDTO(updatedUser));
    }  
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/deactivate")
    public ResponseEntity<UserResponseDTO> deactivateUser(@RequestParam String email) {
        String currentUserEmail = resolveCurrentUserEmail();
        log.warn("Deactivation of {} requested by {}", email, currentUserEmail);
        User updatedUser = userService.deactivateUser(email, currentUserEmail);
        return ResponseEntity.ok(mapToResponseDTO(updatedUser));
    }

    /** Reads the current user's id from the JWT principal and looks up their email. */
    private String resolveCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Authenticated user is required for this action.");
        }
        Long currentUserId;
        Object principal = auth.getPrincipal();
        if (principal instanceof Long l) {
            currentUserId = l;
        } else if (principal instanceof String s) {
            try {
                currentUserId = Long.parseLong(s);
            } catch (NumberFormatException ex) {
                throw new RuntimeException("Invalid authenticated user id.");
            }
        } else {
            throw new RuntimeException("Unsupported authenticated principal type.");
        }
        return userService.getUserById(currentUserId).getEmail();
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/activate")
    public ResponseEntity<UserResponseDTO> activateUser(@RequestParam String email) {
        log.info("Status change requested by authorized personnel for: {}", email);
        User updatedUser = userService.activateUser(email);
        return ResponseEntity.ok(mapToResponseDTO(updatedUser));
    }
   
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponseDTO>> getUsersByRole(@PathVariable com.cognizant.training.feedbacktrack.enums.Role role) {
        log.info("REST request to filter users by role: {}", role);
        List<UserResponseDTO> users = userService.getUsersByRole(role)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
    
    @PutMapping("/profile/change-password")
    public ResponseEntity<String> changePassword(@RequestBody com.cognizant.training.feedbacktrack.dto.ChangePasswordRequest request) {
        log.info("REST request to change password for user: {}", request.getEmail());
        boolean isChanged = userService.changePassword(
                request.getEmail(), 
                request.getOldPassword(), 
                request.getNewPassword()
        );

        if (isChanged) {
            log.info("Password successfully changed for user: {}", request.getEmail());
            return ResponseEntity.ok("Password updated successfully!");
        } else {
            log.error("Password change failed for user: {} (Old password mismatch)", request.getEmail());
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                                 .body("Old password does not match!");
        }
    }
    
    private UserResponseDTO mapToResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUserId(user.getUserId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        if (user.getDepartment() != null) {
            dto.setDepartmentName(user.getDepartment().getDepartmentName());
        }
        
        if (user.getManager() != null) {
            dto.setManagerId(String.valueOf(user.getManager().getUserId()));
        }

        return dto;
    }
    
}