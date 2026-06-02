package com.cognizant.training.feedbacktrack.exceptions;

public class FeedbackCategoryNotFoundException extends RuntimeException{
    public FeedbackCategoryNotFoundException(String msg){
        super(msg);
    }
}
