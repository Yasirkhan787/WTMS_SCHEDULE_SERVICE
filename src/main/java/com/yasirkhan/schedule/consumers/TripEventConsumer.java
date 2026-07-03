package com.yasirkhan.schedule.consumers;

import com.yasirkhan.schedule.models.dtos.TripResponseEventDto.TripResponse;
import com.yasirkhan.schedule.models.dtos.TripResponseEventDto;
import com.yasirkhan.schedule.models.entities.Schedule;
import com.yasirkhan.schedule.repositories.ScheduleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TripEventConsumer {

    private final ScheduleRepository scheduleRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public TripEventConsumer(ScheduleRepository scheduleRepository, RedisTemplate<String, Object> redisTemplate) {
        this.scheduleRepository = scheduleRepository;
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(
            topics = "trip-response-topic",
            groupId = "schedule-group",
            containerFactory = "listenerContainerFactory"
    )
    @Transactional
    public void handleTripCompletion(TripResponseEventDto event) {
        if (!"SUCCESS".equals(event.getEventTypeStatus()) || event.getTripData() == null) {
            log.warn("Ignored unsuccessful or empty trip event.");
            return;
        }

        TripResponse trip = event.getTripData();
        String scheduleId = trip.getScheduleId().toString();
        String tripId = trip.getTripId().toString();

        // ==========================================
        // 1. UPDATE POSTGRESQL: Increment Trip Count
        // ==========================================
        Schedule schedule = scheduleRepository.findById(trip.getScheduleId()).orElse(null);
        if (schedule != null) {
            int currentCount = schedule.getCompletedTrips() != null ? schedule.getCompletedTrips() : 0;
            schedule.setCompletedTrips(currentCount + 1);
            scheduleRepository.save(schedule);
            log.info("Incremented trip count for Schedule {}. Total: {}", scheduleId, schedule.getCompletedTrips());
        } else {
            log.warn("Trip event received for unknown Schedule ID: {}", scheduleId);
        }

        // ==========================================
        // 2. SYNC TO REDIS: Save Trip Data
        // ==========================================
        String tripHashKey = "wtms:trip:" + tripId;
        String scheduleTripsSetKey = "wtms:schedule:" + scheduleId + ":trips";

        // Store the actual trip data in a Hash
        Map<String, String> tripDataMap = new HashMap<>();
        tripDataMap.put("tripId", tripId);
        tripDataMap.put("scheduleId", scheduleId);
        tripDataMap.put("vehicleNo", trip.getVehicleNo());
        tripDataMap.put("slipId", trip.getSlipId());
        tripDataMap.put("loadWeight", String.valueOf(trip.getLoadWeight()));
        tripDataMap.put("emptyWeight", String.valueOf(trip.getEmptyWeight()));
        tripDataMap.put("netWeight", String.valueOf(trip.getNetWeight()));
        tripDataMap.put("fuelConsumed", String.valueOf(trip.getFuelConsumed()));
        tripDataMap.put("actualDistance", String.valueOf(trip.getActualDistance()));
        tripDataMap.put("status", trip.getStatus());

        redisTemplate.opsForHash().putAll(tripHashKey, tripDataMap);

        // Optional: Expire trip data after 48 hours to save memory
        redisTemplate.expire(tripHashKey, 48, TimeUnit.HOURS);

        // Link the Trip to the Schedule in a Redis Set
        redisTemplate.opsForSet().add(scheduleTripsSetKey, tripId);
        redisTemplate.expire(scheduleTripsSetKey, 48, TimeUnit.HOURS);

        log.info("Successfully synced Trip {} to Redis under Schedule {}.", tripId, scheduleId);
    }
}