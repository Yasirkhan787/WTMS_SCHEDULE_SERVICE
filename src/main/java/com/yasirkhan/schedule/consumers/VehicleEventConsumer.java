package com.yasirkhan.schedule.consumers;

import com.yasirkhan.schedule.models.dtos.vehicleResponseEventDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class VehicleEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    public VehicleEventConsumer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(
            topics = "vehicle-response-topic",
            groupId = "schedule-group",
            containerFactory = "listenerContainerFactory"
    )
    public void handleVehicleResponse(vehicleResponseEventDto event) {

        if ("SUCCESS".equals(event.getEventTypeStatus())) {
            String vehicleNo = event.getVehicleNo();

            Map<String, Object> map = new HashMap<>();
            map.put("status", event.getStatus());

            // Save Hash to Redis (Using vehicleNo as the unique identifier)
            String redisKey = "wtms:vehicle:" + vehicleNo;
            redisTemplate.opsForHash().putAll(redisKey, map);
        }
    }
}