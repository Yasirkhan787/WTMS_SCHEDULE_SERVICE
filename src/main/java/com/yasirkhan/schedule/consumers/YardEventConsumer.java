package com.yasirkhan.schedule.consumers;

import com.yasirkhan.schedule.models.dtos.RouteResponseEventDto;
import com.yasirkhan.schedule.models.dtos.YardResponseEventDto;
import com.yasirkhan.schedule.models.enums.EventStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class YardEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    public YardEventConsumer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(
            topics = "yard-response-topic",
            groupId = "schedule-group",
            containerFactory = "listenerContainerFactory"
    )
    public void handleYardResponse(YardResponseEventDto event) {

        if (EventStatus.SUCCESS.equals(event.getEventTypeStatus())) {
            UUID yardId = event.getYardId();

            Map<String, Object> map = new HashMap<>();
            map.put("yardName", event.getYardName());
            map.put("yardType", event.getYardType());
            map.put("boundaryType", event.getBoundaryType());
            map.put("tehsilId", event.getTehsilId());
            map.put("tehsilName",event.getTehsilName());
            map.put("status", event.getStatus());

            // Save Hash to Redis
            String redisKey = "wtms:yard:" + yardId;
            redisTemplate.opsForHash().putAll(redisKey, map);
        }
    }
}