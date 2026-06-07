package com.yasirkhan.schedule.controllers;

import com.yasirkhan.schedule.requests.ScheduleRequest;
import com.yasirkhan.schedule.responses.ScheduleResponse;
import com.yasirkhan.schedule.services.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // Add Schedule
    @PostMapping("/add")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<ScheduleResponse> createSchedule(@RequestBody ScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(request));
    }

    // Update Schedule
    @PatchMapping("/update")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<String> updateSchedule(@RequestBody Map<String, Object> updates){
        scheduleService.updateSchedule(updates);
        return new ResponseEntity<>(
                "Schedule with Id: " + updates.get("scheduleId") + " Updated Successfully",
                HttpStatus.NO_CONTENT
        );
    }

    // Get Schedule
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ScheduleResponse> getScheduleById(@PathVariable UUID id){
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }

    // Get All Schedules
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'DRIVER')")
    public ResponseEntity<List<ScheduleResponse>> getAllSchedule(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(scheduleService.getAllSchedules(status));
    }
}