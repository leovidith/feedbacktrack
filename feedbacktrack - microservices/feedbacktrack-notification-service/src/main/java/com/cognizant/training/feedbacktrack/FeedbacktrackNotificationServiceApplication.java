package com.cognizant.training.feedbacktrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class FeedbacktrackNotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeedbacktrackNotificationServiceApplication.class, args);
        System.out.println("Hello world from notification track!");
	}

}
