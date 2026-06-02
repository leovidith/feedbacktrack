package com.cognizant.training.feedbacktrack.service;

import com.cognizant.training.feedbacktrack.model.FeedbackCategory;
import com.cognizant.training.feedbacktrack.repository.FeedbackCategoryRepository;
import com.cognizant.training.feedbacktrack.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j; // Import for SLF4J
import java.util.List;

@Service
@Slf4j 
public class FeedbackCategoryService {

    @Autowired
    private FeedbackCategoryRepository categoryRepository;

    
    public FeedbackCategory addCategory(FeedbackCategory category) {
        log.info("Admin is adding a new feedback category: {}", category.getCategoryName());
        FeedbackCategory savedCategory = categoryRepository.save(category);
        log.info("Successfully saved category with ID: {}", savedCategory.getCategoryId());
        return savedCategory;
    }

    public FeedbackCategory getCategoryById(Long id) {
        log.info("Fetching category with ID: {}", id);
        return categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category with ID: {} not found", id);
                    return new ResourceNotFoundException("Category not found with ID: " + id);
                });
    }

   
    public FeedbackCategory updateCategory(Long id, FeedbackCategory categoryDetails) {
        log.info("Attempting to update category with ID: {}", id);
        
        FeedbackCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Update failed: Category ID {} not found", id);
                    return new ResourceNotFoundException("Category not found with ID: " + id);
                });
        
        log.debug("Changing category name from '{}' to '{}'", category.getCategoryName(), categoryDetails.getCategoryName());
        
        category.setCategoryName(categoryDetails.getCategoryName());
        category.setDescription(categoryDetails.getDescription());
        
        FeedbackCategory updated = categoryRepository.save(category);
        log.info("Category ID {} updated successfully", id);
        return updated;
    }

    // Used by Employees: Get all configured categories for the feedback form
    public List<FeedbackCategory> getAllCategories() {
        log.info("Fetching all feedback categories for the user form");
        List<FeedbackCategory> categories = categoryRepository.findAll();
        log.debug("Total categories retrieved: {}", categories.size());
        return categories;
    }

    // Admin Action: Remove a category
    public void deleteCategory(Long id) {
        log.warn("Admin requested deletion of category ID: {}", id);
        
        if (!categoryRepository.existsById(id)) {
            log.error("Deletion failed: Category ID {} does not exist", id);
            throw new ResourceNotFoundException("Category not found with ID: " + id);
        }
        
        categoryRepository.deleteById(id);
        log.info("Category ID {} deleted successfully", id);
    }
}