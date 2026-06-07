package com.yasirkhan.schedule.repositories;

import com.yasirkhan.schedule.models.entities.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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

    List<Schedule> findByScheduleDate(LocalDate dispatchDate);

    @Query("SELECT s.vehicleNo FROM Schedule s WHERE s.scheduleDate = :date AND s.template.templateId = :templateId")
    List<String> findBookedVehicleNos(@Param("date") LocalDate date, @Param("templateId") UUID templateId);

    @Query("SELECT s.driverId FROM Schedule s WHERE s.scheduleDate = :date AND s.template.templateId = :templateId")
    List<UUID> findBookedDriverIds(@Param("date") LocalDate date, @Param("templateId") UUID templateId);

    List<Schedule> findByDriverId(UUID driverId);

    List<Schedule> findByRouteIdIn(List<UUID> routeIds);
}
