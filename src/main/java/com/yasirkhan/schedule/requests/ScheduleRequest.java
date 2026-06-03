package com.yasirkhan.schedule.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate scheduleDate;

    private UUID templateId;
}
