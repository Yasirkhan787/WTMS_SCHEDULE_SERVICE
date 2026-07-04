package com.yasirkhan.schedule.consumers;

import com.yasirkhan.schedule.models.dtos.ScheduleUpdateEventDto;
import com.yasirkhan.schedule.services.ScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ScheduleUpdateEventConsumer {

    private final ScheduleService scheduleService;

    public ScheduleUpdateEventConsumer(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @KafkaListener(
            topics = "schedule-update-topic",
            groupId = "schedule-group",
            containerFactory = "listenerContainerFactory"
    )
    public void consumeScheduleUpdate(ScheduleUpdateEventDto eventDto) {
        if (eventDto == null || eventDto.getScheduleId() == null) {
            log.warn("Received empty or null ScheduleUpdateEventDto payload.");
            return;
        }

        log.info("Received preemptive status update DTO from Trip Service for ID: {}", eventDto.getScheduleId());

        try {
            // Map the DTO properties to a dynamic map structure to re-use your robust updateSchedule logic
            Map<String, Object> updates = new HashMap<>();
            updates.put("scheduleId", eventDto.getScheduleId().toString());
            updates.put("shiftStatus", eventDto.getShiftStatus());

            scheduleService.updateSchedule(updates);
            log.info("Successfully processed Kafka DTO update hook for Schedule ID: {}", eventDto.getScheduleId());
        } catch (Exception e) {
            log.error("Failed to update schedule status from DTO: {}", e.getMessage(), e);
        }
    }
}