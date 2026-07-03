package com.yasirkhan.schedule.services.jobs;

import com.yasirkhan.schedule.models.entities.Schedule;
import com.yasirkhan.schedule.models.enums.Status;
import com.yasirkhan.schedule.repositories.ScheduleRepository;
import com.yasirkhan.schedule.services.ScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ScheduleLifecycleJob {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;

    // Industry standard is usually 1 to 2 hours for hardware syncs
    private static final int API_SYNC_GRACE_PERIOD_MINUTES = 60;

    public ScheduleLifecycleJob(ScheduleRepository scheduleRepository, ScheduleService scheduleService) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleService = scheduleService;
    }

    @Scheduled(fixedRate = 900000) // Runs every 15 minutes
    @Transactional
    public void manageScheduleLifecycles() {
        log.info("Running Schedule Lifecycle Watchdog with {} min Reconciliation Window...", API_SYNC_GRACE_PERIOD_MINUTES);

        LocalDateTime currentDateTime = LocalDateTime.now();

        // 1. Fetch all open schedules
        List<Schedule> openSchedules = scheduleRepository.findAll().stream()
                .filter(s -> s.getStatus() == Status.ASSIGNED || s.getStatus() == Status.ACTIVE)
                .toList();

        for (Schedule schedule : openSchedules) {
            LocalDate shiftDate = schedule.getScheduleDate();
            LocalTime shiftStart = schedule.getTemplate().getStartTime();
            LocalTime shiftEnd = schedule.getTemplate().getEndTime();

            // Safely combine Date and Time to handle overnight shifts and grace period rollovers
            LocalDateTime shiftStartDateTime = LocalDateTime.of(shiftDate, shiftStart);
            LocalDateTime shiftEndDateTime = LocalDateTime.of(shiftDate, shiftEnd);

            // Handle Overnight Shifts (e.g., 10 PM to 6 AM next day)
            if (shiftEnd.isBefore(shiftStart)) {
                shiftEndDateTime = shiftEndDateTime.plusDays(1);
            }

            // Calculate the absolute "Dead Drop" time where the ledger officially closes
            LocalDateTime ledgerCloseDateTime = shiftEndDateTime.plusMinutes(API_SYNC_GRACE_PERIOD_MINUTES);

            Status newStatus = null;

            // Scenario 1: Shift is currently ongoing (or in the Grace Period window)
            if (!currentDateTime.isBefore(shiftStartDateTime) && currentDateTime.isBefore(ledgerCloseDateTime)) {
                if (schedule.getStatus() != Status.ACTIVE) {
                    newStatus = Status.ACTIVE; // Moves from ASSIGNED -> ACTIVE
                }
            }
            // Scenario 2: The Reconciliation Window has officially closed
            else if (currentDateTime.isAfter(ledgerCloseDateTime)) {

                if (schedule.getCompletedTrips() != null && schedule.getCompletedTrips() > 0) {
                    newStatus = Status.COMPLETED;
                } else {
                    newStatus = Status.DELAYED;
                }
            }

            // Execute the update
            if (newStatus != null) {
                log.info("Closing Ledger for Schedule {}. Sync Window Passed. New Status: {}", schedule.getScheduleId(), newStatus);

                Map<String, Object> updates = new HashMap<>();
                updates.put("scheduleId", schedule.getScheduleId().toString());
                updates.put("shiftStatus", newStatus.name());

                scheduleService.updateSchedule(updates);
            }
        }
    }
}