package com.yasirkhan.schedule.models.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "WTMS_SCHEDULE")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID scheduleId;

    private String scheduleName;

    private UUID vehicleId;

    private UUID driverId;

    private UUID routeId;

    private Shift shift;

    private LocalDateTime scheduleTime;
}
