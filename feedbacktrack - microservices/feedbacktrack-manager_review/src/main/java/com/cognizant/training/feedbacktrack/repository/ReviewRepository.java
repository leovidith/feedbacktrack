package com.cognizant.training.feedbacktrack.repository;

import com.cognizant.training.feedbacktrack.enums.FeedbackAction;
import com.cognizant.training.feedbacktrack.model.FeedbackReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<FeedbackReview, Long> {

    boolean existsByFeedbackId(Long feedbackId);

    Optional<FeedbackReview> findByFeedbackId(Long feedbackId);

    List<FeedbackReview> findAllByReviewerIdOrderByReviewDateDesc(Long reviewerId);

    List<FeedbackReview> findAllByReviewerIdAndActionTakenNot(Long reviewerId, FeedbackAction action);


    /** Count all reviews grouped by actionTaken for a specific reviewer. */
    @Query("SELECT r.actionTaken, COUNT(r) FROM FeedbackReview r WHERE r.reviewerId = :reviewerId GROUP BY r.actionTaken")
    List<Object[]> countByActionForReviewer(@Param("reviewerId") Long reviewerId);

    /** Count all reviews globally grouped by actionTaken (admin view). */
    @Query("SELECT r.actionTaken, COUNT(r) FROM FeedbackReview r GROUP BY r.actionTaken")
    List<Object[]> countByActionGlobal();



    /** Count reviews per calendar month+year for a specific reviewer within a date range. */
    @Query("SELECT FUNCTION('YEAR', r.reviewDate), FUNCTION('MONTH', r.reviewDate), COUNT(r) " +
            "FROM FeedbackReview r " +
            "WHERE r.reviewerId = :reviewerId AND r.reviewDate BETWEEN :from AND :to " +
            "GROUP BY FUNCTION('YEAR', r.reviewDate), FUNCTION('MONTH', r.reviewDate) " +
            "ORDER BY FUNCTION('YEAR', r.reviewDate), FUNCTION('MONTH', r.reviewDate)")
    List<Object[]> countMonthlyByReviewer(
            @Param("reviewerId") Long reviewerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Count reviews per calendar month+year globally (admin view). */
    @Query("SELECT FUNCTION('YEAR', r.reviewDate), FUNCTION('MONTH', r.reviewDate), COUNT(r) " +
            "FROM FeedbackReview r " +
            "WHERE r.reviewDate BETWEEN :from AND :to " +
            "GROUP BY FUNCTION('YEAR', r.reviewDate), FUNCTION('MONTH', r.reviewDate) " +
            "ORDER BY FUNCTION('YEAR', r.reviewDate), FUNCTION('MONTH', r.reviewDate)")
    List<Object[]> countMonthlyGlobal(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Total reviews for a reviewer in a date window (resolution speed / throughput). */
    long countByReviewerIdAndReviewDateBetween(Long reviewerId, LocalDateTime from, LocalDateTime to);

    /** Reviews in a date window for a reviewer filtered by action. */
    List<FeedbackReview> findAllByReviewerIdAndActionTakenAndReviewDateBetween(
            Long reviewerId, FeedbackAction action, LocalDateTime from, LocalDateTime to);

}
