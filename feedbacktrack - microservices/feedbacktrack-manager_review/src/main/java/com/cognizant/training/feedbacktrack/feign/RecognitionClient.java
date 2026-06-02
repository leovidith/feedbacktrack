package com.cognizant.training.feedbacktrack.feign;

import com.cognizant.training.feedbacktrack.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "feedbacktrack-recognition-service", configuration = FeignClientConfig.class)
public interface RecognitionClient {

	@GetMapping("/api/v1/recognition/points/target/{targetUserId}")
	Integer sumPointsByTargetUserId(@PathVariable("targetUserId") Long targetUserId);
}
