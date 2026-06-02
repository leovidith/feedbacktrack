package com.cognizant.training.feedbacktrack.repository;

import com.cognizant.training.feedbacktrack.model.FeedbackCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FeedbackCategoryRepository extends JpaRepository<FeedbackCategory, Long> {
    
    // Custom query to find a category by its name (useful for the Admin seeding logic)
    Optional<FeedbackCategory> findByCategoryName(String categoryName);
}