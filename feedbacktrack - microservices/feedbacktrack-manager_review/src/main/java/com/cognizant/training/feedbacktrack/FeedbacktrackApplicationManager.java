package com.cognizant.training.feedbacktrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class FeedbacktrackApplicationManager {

	public static void main(String[] args) {

        SpringApplication.run(FeedbacktrackApplicationManager.class, args);
        System.out.println("Hello World from manager review track!");
	}

}
