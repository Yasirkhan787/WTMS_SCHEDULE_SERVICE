package com.yasirkhan.schedule.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class ShiftTemplateResponse {

    private UUID templateId;

    private String shiftName;

    private LocalTime startTime;

    private LocalTime endTime;

    private String remarks;
}
