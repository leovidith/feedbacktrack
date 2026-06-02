package com.cognizant.training.feedbacktrack.controller;

import com.cognizant.training.feedbacktrack.config.JwtAuthenticationFilter;
import com.cognizant.training.feedbacktrack.dto.NotificationRequest;
import com.cognizant.training.feedbacktrack.enums.NotificationType;
import com.cognizant.training.feedbacktrack.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = NotificationController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        },
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = JwtAuthenticationFilter.class
                )
        }

)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sendNotification_shouldReturnOk() throws Exception {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(1L);
        request.setMessage("Test");
        request.setType(NotificationType.ACKNOWLEDGED);
        request.setSourceId(1L);

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Mockito.verify(service)
                .sendNotification(1L, "Test", NotificationType.ACKNOWLEDGED,1L);
    }

    @Test
    void getNotifications_shouldReturnOkAndList() throws Exception {

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Authentication authentication = Mockito.mock(Authentication.class);

        Mockito.when(authentication.getPrincipal()).thenReturn(1L);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        Mockito.when(service.getUserNotifications(1L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        Mockito.verify(service).getUserNotifications(1L);
    }

    @Test
    void markRead_shouldReturnOk() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/read/1"))
                .andExpect(status().isOk());

        Mockito.verify(service).markAsRead(1L);
    }
}
