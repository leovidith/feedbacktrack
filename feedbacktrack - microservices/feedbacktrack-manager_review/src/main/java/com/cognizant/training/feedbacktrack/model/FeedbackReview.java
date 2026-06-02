package com.cognizant.training.feedbacktrack.model;

import com.cognizant.training.feedbacktrack.enums.FeedbackAction;
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
@Table(name = "feedback_reviews")
public class FeedbackReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @Column(nullable = false, unique = true)
    @NotNull(message = "Feedback ID is required")
    private Long feedbackId;

    @Column(nullable = false)
    @NotNull(message = "Reviewer (Manager) ID is required")
    private Long reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackAction actionTaken;

    @Size(max = 1000)
    private String managerNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime reviewDate = LocalDateTime.now();
}
