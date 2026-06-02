package com.cognizant.training.feedbacktrack.client;

import com.cognizant.training.feedbacktrack.dto.UserResponseDTO;
import com.cognizant.training.feedbacktrack.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "feedbacktrack-auth-service", contextId = "UserServiceClient", configuration = FeignClientConfig.class)
public interface UserClient {

    @GetMapping("/api/admin/users/{id}")
    UserResponseDTO getUserById(@PathVariable("id") Long id);

}



