package com.yasirkhan.schedule.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleRequest {

    private String scheduleName;

    private String vehicleNo;

    private UUID driverId;

    private UUID routeId;

    private LocalDate scheduleDate;

    private UUID templateId;
}
