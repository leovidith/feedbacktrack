package com.cognizant.training.feedbacktrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cognizant.training.feedbacktrack.exception.ResourceNotFoundException;
import com.cognizant.training.feedbacktrack.model.FeedbackCategory;
import com.cognizant.training.feedbacktrack.repository.FeedbackCategoryRepository;

@ExtendWith(MockitoExtension.class)
public class FeedbackCategoryServiceTest {

    @Mock
    private FeedbackCategoryRepository categoryRepository;

    @InjectMocks
    private FeedbackCategoryService categoryService;

    @Test
    void testAddCategory() {
        FeedbackCategory cat = new FeedbackCategory(null, "Technical", "Coding skills");
        when(categoryRepository.save(cat)).thenReturn(cat);

        FeedbackCategory result = categoryService.addCategory(cat);
        assertEquals("Technical", result.getCategoryName());
    }

    @Test
    void testUpdateCategory_NotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> categoryService.updateCategory(1L, new FeedbackCategory()));
    }
}