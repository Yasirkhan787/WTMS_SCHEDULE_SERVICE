package com.yasirkhan.schedule.responses;

import com.yasirkhan.schedule.models.entities.Shift;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ScheduleResponse {

    private UUID scheduleId;

    private String scheduleName;

    private UUID vehicleId;

    private UUID driverId;

    private UUID routeId;

    private Shift shift;

    private LocalDateTime scheduleTime;
}
