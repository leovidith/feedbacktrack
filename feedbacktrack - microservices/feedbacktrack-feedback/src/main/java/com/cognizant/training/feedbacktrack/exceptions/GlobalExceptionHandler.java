package com.cognizant.training.feedbacktrack.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FeedbackNotFoundException.class)
    public ResponseEntity<String> handleFeedbackNotFound(FeedbackNotFoundException fe){
        return new ResponseEntity<>(fe.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidFeedbackException.class)
    public ResponseEntity<String> handleBadRequest(InvalidFeedbackException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FeedbackCategoryNotFoundException.class)
    public ResponseEntity<String> categoryNotFound(FeedbackCategoryNotFoundException fe){
        return new ResponseEntity<>(fe.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<String> unautorizedAccess(UnauthorizedAccessException ue){
        return new ResponseEntity<>(ue.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException ue){
        return new ResponseEntity<>(ue.getMessage(), HttpStatus.NOT_FOUND);
    }
}
