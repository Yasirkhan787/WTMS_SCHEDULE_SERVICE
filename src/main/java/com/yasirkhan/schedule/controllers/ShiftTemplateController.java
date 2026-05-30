package com.yasirkhan.schedule.controllers;

import com.yasirkhan.schedule.requests.ShiftTemplateRequest;
import com.yasirkhan.schedule.responses.ShiftTemplateResponse;
import com.yasirkhan.schedule.services.ShiftTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/schedule/shift")
public class ShiftTemplateController {

    private final ShiftTemplateService shiftTemplateService;

    public ShiftTemplateController(ShiftTemplateService shiftTemplateService) {
        this.shiftTemplateService = shiftTemplateService;
    }

    // Add new shift
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')") // FIXED: Removed '=' and fixed SpEL syntax
    public ResponseEntity<ShiftTemplateResponse> createShiftTemplate(@RequestBody ShiftTemplateRequest shiftTemplateRequest) { // FIXED: Added @RequestBody
        return new ResponseEntity<>(shiftTemplateService.createShiftTemplate(shiftTemplateRequest),
                HttpStatus.CREATED);
    }

    // Update Schedule
    @PatchMapping("/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')") // FIXED syntax
    public ResponseEntity<String> updateSchedule(@RequestBody Map<String, Object> updates){
        shiftTemplateService.updateShiftTemplate(updates);
        return new ResponseEntity<>("Shift Template with Id: " + updates.get("shiftTemplateId") + " Updated Successfully",
                HttpStatus.NO_CONTENT);
    }

    // Get Schedule
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')") // FIXED syntax
    public ResponseEntity<ShiftTemplateResponse> getScheduleById(@PathVariable UUID id){ // FIXED: Changed to @PathVariable
        return ResponseEntity.ok(shiftTemplateService.getShiftTemplateById(id));
    }

    // Get All Schedules
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')") // FIXED syntax
    public ResponseEntity<List<ShiftTemplateResponse>> getAllSchedule(){
        return ResponseEntity.ok(shiftTemplateService.getShiftTemplates());
    }
}