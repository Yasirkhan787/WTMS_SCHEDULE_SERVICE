package com.yasirkhan.schedule.controllers;

import com.yasirkhan.schedule.requests.ScheduleRequest;
import com.yasirkhan.schedule.responses.ScheduleResponse;
import com.yasirkhan.schedule.services.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/add")
    public ResponseEntity<ScheduleResponse> createSchedule(@RequestBody ScheduleRequest request) {
        return ResponseEntity
                .ok(scheduleService.createSchedule(request));
    }

    @PatchMapping("/update")
    public ResponseEntity<String> updateSchedule(@RequestBody Map<String, Object> updates){

        scheduleService.updateSchedule(updates);

        return new ResponseEntity
                ("Schedule with Id: " + updates.get("scheduleId") + "Updated Successfully",
                        HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> getScheduleById(@RequestParam UUID id){

        return ResponseEntity
                .ok(scheduleService.getScheduleById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ScheduleResponse>> getAllSchedule(){

        return ResponseEntity
                .ok(scheduleService.getAllSchedule());
    }
}
