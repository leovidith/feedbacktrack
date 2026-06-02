package com.cognizant.training.feedbacktrack.controller;

import com.cognizant.training.feedbacktrack.model.FeedbackCategory;
import com.cognizant.training.feedbacktrack.service.FeedbackCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j; 
import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@Slf4j 
public class FeedbackCategoryController {

    @Autowired
    private FeedbackCategoryService categoryService;

   
    @PostMapping("/add")
    public ResponseEntity<FeedbackCategory> addCategory(@RequestBody FeedbackCategory category) {
        log.info("REST request to add a new feedback category: {}", category.getCategoryName());
        FeedbackCategory savedCategory = categoryService.addCategory(category);
        log.info("Successfully created category with ID: {}", savedCategory.getCategoryId());
        return ResponseEntity.ok(savedCategory);
    }

    
    @GetMapping("/all")
    public ResponseEntity<List<FeedbackCategory>> getAllCategories() {
        log.info("REST request to fetch all feedback categories");
        List<FeedbackCategory> categories = categoryService.getAllCategories();
        log.debug("Returning {} categories to the requester", categories.size());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackCategory> getCategoryById(@PathVariable Long id) {
        log.info("REST request to fetch category by ID: {}", id);
        FeedbackCategory category = categoryService.getCategoryById(id);
        if (category != null) {
            log.info("Category found: {}", category.getCategoryName());
            return ResponseEntity.ok(category);
        } else {
            log.warn("Category with ID: {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }

    
    @PutMapping("/{id}")
    public ResponseEntity<FeedbackCategory> updateCategory(@PathVariable Long id, @RequestBody FeedbackCategory category) {
        log.info("REST request to update category ID: {}", id);
        FeedbackCategory updatedCategory = categoryService.updateCategory(id, category);
        log.info("Category ID: {} updated successfully", id);
        return ResponseEntity.ok(updatedCategory);
    }

    
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        log.warn("REST request to DELETE category ID: {}", id);
        categoryService.deleteCategory(id);
        log.info("Category ID: {} successfully deleted", id);
        return ResponseEntity.ok("Category deleted successfully.");
    }
}