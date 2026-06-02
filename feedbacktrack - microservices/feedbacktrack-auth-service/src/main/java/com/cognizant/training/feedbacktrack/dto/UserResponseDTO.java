package com.cognizant.training.feedbacktrack.dto;

import com.cognizant.training.feedbacktrack.enums.Role;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserResponseDTO {
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