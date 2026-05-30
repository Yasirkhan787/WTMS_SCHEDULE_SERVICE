package com.yasirkhan.schedule.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
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

    private String vehicleNo;

    private UUID driverId;

    private UUID routeId;

    private LocalDate scheduleDate;

    // Many dispatches/schedules can use the exact same Shift Template
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id", referencedColumnName = "templateId")
    private ShiftTemplate template;

    private Status shiftStatus;
}