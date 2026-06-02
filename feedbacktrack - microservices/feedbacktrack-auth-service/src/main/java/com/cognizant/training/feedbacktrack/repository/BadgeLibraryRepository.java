package com.cognizant.training.feedbacktrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cognizant.training.feedbacktrack.model.BadgeLibrary;
import org.springframework.stereotype.Repository;

@Repository
public interface BadgeLibraryRepository extends JpaRepository<BadgeLibrary, Long> {
   
    boolean existsByBadgeName(String badgeName);
}