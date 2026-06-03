package com.yasirkhan.schedule.consumers;

import com.yasirkhan.schedule.models.dtos.RouteResponseEventDto;
import com.yasirkhan.schedule.models.enums.EventStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RouteEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    public RouteEventConsumer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(
            topics = "route-response-topic",
            groupId = "schedule-group",
            containerFactory = "listenerContainerFactory"
    )
    public void handleRouteResponse(RouteResponseEventDto event) {

        if (EventStatus.SUCCESS.equals(event.getEventTypeStatus())) {
            UUID routeId = event.getRouteId();

            Map<String, Object> map = new HashMap<>();
            map.put("origin", event.getOrigin());
            map.put("destination", event.getDestination());
            map.put("status", event.getStatus());

            // Save Hash to Redis
            String redisKey = "wtms:route:" + routeId;
            redisTemplate.opsForHash().putAll(redisKey, map);
        }
    }
}