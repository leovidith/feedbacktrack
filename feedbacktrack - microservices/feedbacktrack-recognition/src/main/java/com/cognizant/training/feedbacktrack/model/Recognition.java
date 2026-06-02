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
@Table(name = "recognitions")
public class Recognition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recognitionId;

    @Column(nullable = false)
    @NotNull(message = "Sender ID is required")
    private Long senderId; // Reference to User Service

    @Column(nullable = false)
    @NotNull(message = "Target User ID is required")
    private Long targetUserId; // Reference to User Service

    @Column(nullable = false)
    private Long badgeId;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Size(min = 5, max = 500, message = "Message must be between 5 and 500 characters")
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime recognizedDate = LocalDateTime.now();
}
