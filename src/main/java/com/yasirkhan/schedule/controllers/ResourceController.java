package com.yasirkhan.schedule.controllers;

import com.yasirkhan.schedule.requests.AvailableAssetRequest;
import com.yasirkhan.schedule.responses.AvailableAssetResponse;
import com.yasirkhan.schedule.responses.AvailableResourceResponse;
import com.yasirkhan.schedule.responses.ResourceResponse;
import com.yasirkhan.schedule.services.ResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedule/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping("/available-assets")
    public ResponseEntity<AvailableAssetResponse> getAvailableAssetsForDispatch(@RequestBody AvailableAssetRequest request) {
        return ResponseEntity.ok(resourceService.getAvailableAssets(request));
    }

    @GetMapping
    public ResponseEntity<AvailableResourceResponse> getAllResources() {
        return ResponseEntity.ok(resourceService.getAvailableResources());
    }
}

