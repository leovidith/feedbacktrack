package com.cognizant.training.feedbacktrack.dto;

import com.cognizant.training.feedbacktrack.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long userId;
    private String name;
    private String email;
    private Role role;
    private String departmentName;
    private String status;
    private String managerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

