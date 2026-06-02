package com.cognizant.training.feedbacktrack.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cognizant.training.feedbacktrack.model.BadgeLibrary;
import com.cognizant.training.feedbacktrack.repository.BadgeLibraryRepository;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Service
@Slf4j
public class BadgeLibraryService {

    @Autowired
    private BadgeLibraryRepository badgeRepo;

    /**
     * Adds a new badge to the library. 
     * Includes a check to prevent duplicate badge names.
     */
    public BadgeLibrary addBadge(BadgeLibrary badge) {
        if (badgeRepo.existsByBadgeName(badge.getBadgeName())) {
            log.error("Failed to add badge: {} already exists", badge.getBadgeName());
            throw new RuntimeException("Badge name '" + badge.getBadgeName() + "' already exists in the library.");
        }
        log.info("Admin added a new badge: {}", badge.getBadgeName());
        return badgeRepo.save(badge);
    }

    /**
     * Retrieves all available badges for users to choose from.
     */
    public List<BadgeLibrary> getAllBadges() {
        log.info("Fetching all badges for the library");
        return badgeRepo.findAll();
    }

    /**
     * Finds a specific badge by ID.
     */
    public BadgeLibrary getBadgeById(Long id) {
        return badgeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Badge not found with ID: " + id));
    }

    /**
     * Updates an existing badge's details (e.g., changing points or description).
     */
    public BadgeLibrary updateBadge(Long id, BadgeLibrary details) {
        BadgeLibrary existingBadge = getBadgeById(id);
        existingBadge.setBadgeName(details.getBadgeName());
        existingBadge.setPointsValue(details.getPointsValue());
        existingBadge.setDescription(details.getDescription());
        existingBadge.setBadgeIconPath(details.getBadgeIconPath());
        
        log.info("Updated badge ID: {}", id);
        return badgeRepo.save(existingBadge);
    }

    /**
     * Deletes a badge from the library.
     */
    public void deleteBadge(Long id) {
        if (!badgeRepo.existsById(id)) {
            throw new RuntimeException("Cannot delete: Badge ID " + id + " does not exist.");
        }
        log.warn("Admin is deleting badge ID: {}", id);
        badgeRepo.deleteById(id);
    }
}