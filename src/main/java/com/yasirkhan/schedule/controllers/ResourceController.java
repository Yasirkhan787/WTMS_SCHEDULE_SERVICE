package com.yasirkhan.schedule.controllers;

import com.yasirkhan.schedule.responses.ResourceResponse;
import com.yasirkhan.schedule.services.ResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ResponseEntity<ResourceResponse> getAllResources() {
        return ResponseEntity.ok(resourceService.getAllResources());
    }
}

