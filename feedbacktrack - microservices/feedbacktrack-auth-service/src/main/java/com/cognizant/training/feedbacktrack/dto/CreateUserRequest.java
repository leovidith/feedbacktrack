package com.cognizant.training.feedbacktrack.dto;

import com.cognizant.training.feedbacktrack.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    /**
     * Plain-text password – will be encoded by the service before persistence.
     * If not supplied a default temporary password is used.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Role is required (EMPLOYEE, MANAGER, ADMIN)")
    private Role role;

    /** ID of the department this user belongs to (resolved via Department Service). */
    private Long departmentId;

    /** ID of this user's direct manager (must exist in the User DB). */
    private Long managerId;

    /** Initial status – defaults to 'Active' in the service if not supplied. */
    private String status;
}

