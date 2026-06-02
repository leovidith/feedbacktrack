package com.cognizant.training.feedbacktrack;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cognizant.training.feedbacktrack.enums.Role;
import com.cognizant.training.feedbacktrack.model.FeedbackCategory;
import com.cognizant.training.feedbacktrack.model.User;
import com.cognizant.training.feedbacktrack.repository.FeedbackCategoryRepository;
import com.cognizant.training.feedbacktrack.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j

@EnableFeignClients(basePackages = "com.cognizant.training.feedbacktrack.client")
@EnableDiscoveryClient
public class FeedbacktrackAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedbacktrackAuthServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner bootstrap(UserRepository userRepository, PasswordEncoder encoder,FeedbackCategoryRepository categoryRepository) {
        return args -> {
        	if (categoryRepository.count() == 0) {
                categoryRepository.save(new FeedbackCategory(null, "Technical Skills", "Coding and logic"));
                categoryRepository.save(new FeedbackCategory(null, "Behavioral", "Soft skills and teamwork"));
               log.info(">>> System Categories Configured.");
            }
            if (userRepository.findByEmail("admin@feedbacktrack.com").isEmpty()) {
                User admin = new User();
                admin.setName("Somiya Singh");
                admin.setEmail("admin@feedbacktrack.com");
                admin.setPassword(encoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                admin.setStatus("Active");
                userRepository.save(admin);
               log.info(">>> Super Admin initialized.");
            }else {
            	log.info("Super Admin already exists. Skipping initialization.");
            }
        };
    }
}