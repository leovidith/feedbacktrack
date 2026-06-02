package com.cognizant.training.feedbacktrack.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<String> unauthorizedAccess(UnauthorizedAccessException ue){
        log.warn("Unauthorized access attempt: {}", ue.getMessage());
        return new ResponseEntity<>(ue.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InvalidRecognitionException.class)
    public ResponseEntity<String> unauthorizedAccess(InvalidRecognitionException ue){
        log.warn("Invalid recognition request: {}", ue.getMessage());
        return new ResponseEntity<>(ue.getMessage(), HttpStatus.FORBIDDEN);
    }


    @ExceptionHandler(RecognitionNotFoundException.class)
    public ResponseEntity<String> handleFeedbackNotFound(RecognitionNotFoundException fe){
        log.warn("Recognition not found: {}", fe.getMessage());
        return new ResponseEntity<>(fe.getMessage(), HttpStatus.NOT_FOUND);
    }
}
