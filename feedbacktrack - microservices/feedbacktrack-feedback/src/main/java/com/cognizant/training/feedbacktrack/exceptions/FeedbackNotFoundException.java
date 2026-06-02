package com.cognizant.training.feedbacktrack.exceptions;

public class FeedbackNotFoundException extends RuntimeException {
    public FeedbackNotFoundException(){
        super();
    }

    public FeedbackNotFoundException(String msg){
        super(msg);
    }
}
