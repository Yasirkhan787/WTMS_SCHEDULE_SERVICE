package com.yasirkhan.schedule.requests;

import com.yasirkhan.schedule.models.entities.Shift;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Builder
public class ScheduleRequest {

    private String scheduleName;

    private UUID vehicleId;

    private UUID driverId;

    private UUID routeId;

    private Shift shift;

    private LocalDateTime scheduleTime;
}
