package com.cognizant.training.feedbacktrack.controller;

import com.cognizant.training.feedbacktrack.dto.*;
import com.cognizant.training.feedbacktrack.model.Recognition;
import com.cognizant.training.feedbacktrack.service.RecognitionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/recognition")
@Slf4j
public class RecognitionController {

    @Autowired
    private RecognitionService recognitionService;


    @PostMapping
    public ResponseEntity<Recognition> create(@Valid @RequestBody CreateRecognitionRequest request) {
        log.info("REST request to create recognition for target {}", request.getTargetUserId());
        Recognition recognition = recognitionService.create(request);
        return new ResponseEntity<>(recognition, HttpStatus.CREATED);
    }

    @GetMapping("/received/{userId}") // Fixed spelling from "recieved"
    public ResponseEntity<List<Recognition>> findReceived(@PathVariable Long userId) {
        //Get the userId from security or jwt in the backend(look at old role-based repo)
        log.info("REST request to fetch received recognitions for user: {}", userId);
        List<Recognition> recognitions = recognitionService.findReceived(userId);
        return ResponseEntity.ok(recognitions);
    }

    @GetMapping("/sent/{userId}")
    public ResponseEntity<List<Recognition>> findSent(@PathVariable Long userId) {
        //Get the userId from security or jwt in the backend(look at old role-based repo)
        log.info("REST request to fetch sent recognitions for user: {}", userId);
        List<Recognition> recognitions = recognitionService.findSent(userId);
        return ResponseEntity.ok(recognitions);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("REST request to delete recognition ID: {}", id);
        recognitionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/manager/{managerId}")
    public ResponseEntity<List<Recognition>> getTeamRecognitions(@PathVariable Long managerId) {
        //Get the userId from security or jwt in the backend(look at old role-based repo)
        log.info("REST request for team recognitions under manager: {}", managerId);
        List<Recognition> recognitions = recognitionService.getTeamRecognitions(managerId);
        return ResponseEntity.ok(recognitions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recognition> viewRecognition(@PathVariable Long id) {
        return ResponseEntity.ok(recognitionService.viewRecognition(id));
    }

    @GetMapping("/feed")
    public ResponseEntity<List<Recognition>> viewAll() {
        log.info("REST request to view all recognitions (feed)");
        return ResponseEntity.ok(recognitionService.viewAll());
    }

    @GetMapping("/points/target/{targetUserId}")
    public ResponseEntity<Integer> sumPointsByTargetUserId(@PathVariable Long targetUserId) {
        log.info("REST request to sum recognition points for user: {}", targetUserId);
        Integer totalPoints = recognitionService.sumPointsByTargetUserId(targetUserId);
        return ResponseEntity.ok(totalPoints);
    }
}
