package com.cognizant.training.feedbacktrack.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getCredentials() != null) {
            String token = auth.getCredentials().toString();
            template.header("Authorization", "Bearer " + token);
        }
    }
}