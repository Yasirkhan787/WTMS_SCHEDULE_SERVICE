package com.yasirkhan.schedule.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yasirkhan.schedule.models.enums.Status;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate scheduleDate;
    private Status scheduleStatus;

    // Shift Template Data (PostgreSQL Joined)
    private UUID templateId;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;

    // Redis Enriched Data
    private UUID tehsilId;
    private String tehsilName;
    private UUID driverId;
    private String driverName;
    private String driverPhoneNo;
    private String driverStatus;
    private String vehicleNo;
    private String vehicleStatus;
    private UUID routeId;
    private String routeName;
    private String routePath;

    // destination Yard details
    private String destinationYardId;
    private String destinationYardName;
    private String destinationYardType;
    private String destinationYardBoundaryType;
    private String destinationYardRadiusMeters;
    private String destinationYardPolygonPath;
    private String destinationYardCenterLat;
    private String destinationYardCenterLng;
}
