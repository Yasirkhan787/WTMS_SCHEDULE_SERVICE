package com.yasirkhan.schedule.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "WTMS_SHIFTS_TEMPLATE")
public class ShiftTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID templateId;

    private String shiftName;

    private LocalTime startTime;

    private LocalTime endTime;

    private String remarks;

    private Status status;

}
