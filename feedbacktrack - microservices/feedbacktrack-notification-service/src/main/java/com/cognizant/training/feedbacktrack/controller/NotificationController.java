package com.cognizant.training.feedbacktrack.controller;

import com.cognizant.training.feedbacktrack.dto.NotificationRequest;
import com.cognizant.training.feedbacktrack.dto.NotificationResponse;
import com.cognizant.training.feedbacktrack.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "APIs for sending and managing user notifications")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    @Autowired
    private NotificationService service;

    @Operation(
            summary = "Send a notification",
            description = "Sends a notification to a specific user with a message, type, and source reference."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT missing or invalid")
    })
    @PostMapping
    public ResponseEntity<String> sendNotification(@RequestBody @Valid NotificationRequest request) {
        service.sendNotification(
                request.getUserId(),
                request.getMessage(),
                request.getType(),
                request.getSourceId()
        );
        return ResponseEntity.ok("Notification sent!");
    }

    @Operation(
            summary = "Get notifications for authenticated user",
            description = "Retrieves all notifications belonging to the currently authenticated user extracted from the JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of notifications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT missing or invalid")
    })
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications() {
        Long userId = (Long) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        List<NotificationResponse> notifications = service.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @Operation(
            summary = "Mark a notification as read",
            description = "Marks a specific notification as read by its ID. The ID must be a positive number."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "400", description = "Invalid ID - must be a positive number"),
            @ApiResponse(responseCode = "404", description = "Notification not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/read/{id}")
    public ResponseEntity<String> markRead(
            @Parameter(description = "ID of the notification to mark as read", required = true)
            @PathVariable @Positive Long id) {
        service.markAsRead(id);
        return ResponseEntity.ok("Marked as read");
    }
}