package com.yasirkhan.schedule.requests;

import lombok.Data;

import java.time.LocalTime;

@Data
public class ShiftTemplateRequest {

    private String shiftName;

    private LocalTime startTime;

    private LocalTime endTime;

    private String remarks;
}
