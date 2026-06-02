package com.cognizant.training.feedbacktrack.repository;

import com.cognizant.training.feedbacktrack.model.Recognition;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecognitionRepository extends JpaRepository<Recognition, Long> {

    // Replaces findByTargetUser_UserId
    List<Recognition> findAllByTargetUserId(Long targetUserId);

    // Replaces findBySender_UserId
    List<Recognition> findAllBySenderId(Long senderId);

    // Used for Managerial team lookups
    List<Recognition> findAllByTargetUserIdIn(List<Long> targetUserIds);
}
