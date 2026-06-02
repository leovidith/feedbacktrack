package com.cognizant.training.feedbacktrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Entity
@Table(name = "feedbacks")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feedbackId;

    @Column(nullable = false)
    @NotNull(message = "Sender ID is required")
    private Long senderId; // Reference to User Service

    @Column(nullable = false)
    @NotNull(message = "Target User ID is required")
    private Long targetUserId; // Reference to User Service

    @Column(nullable = false)
    @NotNull(message = "Feedback category is required")
    private Long categoryId; // Reference to FeedbackCategory Service

    @Column(columnDefinition = "TEXT", nullable = false)
    @Size(min = 10, max = 2000, message = "Comments must be between 10 and 2000 characters")
    private String comments;

    @Column(nullable = false)
    private boolean isAnonymous = false;

    private LocalDateTime submittedDate=LocalDateTime.now();
}
