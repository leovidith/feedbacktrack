package com.cognizant.training.feedbacktrack.exceptions;

public class UnauthorizedAccessException extends RuntimeException{
    public UnauthorizedAccessException(String msg){
        super(msg);
    }
}
