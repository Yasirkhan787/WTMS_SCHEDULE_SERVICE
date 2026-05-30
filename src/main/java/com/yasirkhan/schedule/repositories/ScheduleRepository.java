package com.yasirkhan.schedule.repositories;

import com.yasirkhan.schedule.models.entities.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    // Checks if the vehicle is already booked for this specific template on this exact date
    boolean existsByVehicleNoAndScheduleDateAndTemplate_TemplateId(
            String vehicleNo,
            LocalDate scheduleDate,
            UUID templateId
    );

    // Checks if the driver is already booked for this specific template on this exact date
    boolean existsByDriverIdAndScheduleDateAndTemplate_TemplateId(
            UUID driverId,
            LocalDate scheduleDate,
            UUID templateId
    );
}