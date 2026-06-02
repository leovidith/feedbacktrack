package com.cognizant.training.feedbacktrack.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback")
public class Route {

    @GetMapping("/path")
    public String path(){
        return "This is the first route";
    }
}
