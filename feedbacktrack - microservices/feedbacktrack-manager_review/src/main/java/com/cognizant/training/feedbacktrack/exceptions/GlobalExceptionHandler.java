package com.cognizant.training.feedbacktrack.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<String> unautorizedAccess(UnauthorizedAccessException ue){
        return new ResponseEntity<>(ue.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<String> handleReviewNotFound(ReviewNotFoundException re){
        return new ResponseEntity<>(re.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException ie){
        return new ResponseEntity<>(ie.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException iae){
        return new ResponseEntity<>(iae.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handleNotImplemented(UnsupportedOperationException uoe){
        return new ResponseEntity<>(uoe.getMessage(), HttpStatus.NOT_IMPLEMENTED);
    }

}
