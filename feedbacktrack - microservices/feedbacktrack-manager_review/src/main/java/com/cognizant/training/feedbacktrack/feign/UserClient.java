package com.cognizant.training.feedbacktrack.feign;

import com.cognizant.training.feedbacktrack.config.FeignClientConfig;
import com.cognizant.training.feedbacktrack.dto.UserResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "feedbacktrack-auth-service", configuration = FeignClientConfig.class, contextId = "UserServiceClient")
public interface UserClient {

    @GetMapping("/api/admin/users/{id}")
    UserResponseDTO getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/admin/users/manager/{managerId}/team")
    List<UserResponseDTO> getTeamMembersByManagerId(@PathVariable("managerId") Long managerId);
}



