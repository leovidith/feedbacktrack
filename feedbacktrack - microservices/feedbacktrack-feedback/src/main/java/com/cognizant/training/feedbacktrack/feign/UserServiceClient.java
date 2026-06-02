package com.cognizant.training.feedbacktrack.feign;

import com.cognizant.training.feedbacktrack.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "feedbacktrack-auth-service", contextId = "UserServiceClient")
public interface UserServiceClient {

    @GetMapping("/api/admin/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/admin/users/manager/{managerId}/team")
    List<UserDTO> getTeamMembersByManagerId(@PathVariable("managerId") Long managerId);
}