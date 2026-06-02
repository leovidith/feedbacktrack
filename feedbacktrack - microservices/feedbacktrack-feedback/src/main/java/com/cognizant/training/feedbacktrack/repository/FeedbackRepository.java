package com.cognizant.training.feedbacktrack.repository;

import com.cognizant.training.feedbacktrack.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByTargetUserIdIn(List<Long> targetUserIds);
    long countByTargetUserId(Long targetUserId);

    List<Feedback> findAllBySenderIdOrderBySubmittedDateDesc(Long senderId);
    List<Feedback> findAllByTargetUserIdOrderBySubmittedDateDesc(Long targetUserId);
}
