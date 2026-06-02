package com.cognizant.training.feedbacktrack.exceptions;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(String msg){
        super(msg);
    }
}
