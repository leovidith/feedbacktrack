package com.cognizant.training.feedbacktrack.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.cognizant.training.feedbacktrack.model.BadgeLibrary;
import com.cognizant.training.feedbacktrack.service.BadgeLibraryService;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@RestController
@RequestMapping("/api/admin/badges")
//@CrossOrigin("http://localhost:4200")
@Slf4j
public class BadgeLibraryController {

    @Autowired
    private BadgeLibraryService badgeService;

    
    @GetMapping("/all")
    public ResponseEntity<List<BadgeLibrary>> getAllBadges() {
        log.info("Fetching all available badges from the library.");
        return ResponseEntity.ok(badgeService.getAllBadges());
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<BadgeLibrary> getBadgeById(@PathVariable Long id) {
        return ResponseEntity.ok(badgeService.getBadgeById(id));
    }

    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<BadgeLibrary> createBadge(@RequestBody BadgeLibrary badge) {
        log.warn("ADMIN ACTION: Adding a new badge to the library: {}", badge.getBadgeName());
        return ResponseEntity.ok(badgeService.addBadge(badge));
    }

   
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update/{id}")
    public ResponseEntity<BadgeLibrary> updateBadge(@PathVariable Long id, @RequestBody BadgeLibrary badgeDetails) {
        log.warn("ADMIN ACTION: Updating badge ID: {}", id);
        return ResponseEntity.ok(badgeService.updateBadge(id, badgeDetails));
    }

   
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteBadge(@PathVariable Long id) {
        log.warn("ADMIN ACTION: Deleting badge ID: {}", id);
        badgeService.deleteBadge(id);
        return ResponseEntity.ok("Badge successfully removed from the library.");
    }
}