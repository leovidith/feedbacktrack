package com.cognizant.training.feedbacktrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class FeedbacktrackApplicationRecognition {

	public static void main(String[] args) {

        SpringApplication.run(FeedbacktrackApplicationRecognition.class, args);
        System.out.println("Hello world from recognition track!");
	}
}
