package com.yasirkhan.schedule.responses;

import com.yasirkhan.schedule.models.entities.ShiftTemplate;
import com.yasirkhan.schedule.models.entities.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleResponse {

    // PostgreSQL Data
    private UUID scheduleId;
    private String scheduleName;
    private LocalDate scheduleDate;
    private Status shiftStatus;

    // Shift Template Data (PostgreSQL Joined)
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;

    // Redis Enriched Data
    private String name;
    private String phoneNo;
    private String driverStatus;
    private String vehicleNo;
    private String vehicleStatus;
    private String routeOrigin;
    private String routeDestination;

}
