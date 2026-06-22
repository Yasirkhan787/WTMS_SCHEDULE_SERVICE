package com.yasirkhan.schedule.consumers;

import com.yasirkhan.schedule.models.dtos.UserResponseEventDto;
import com.yasirkhan.schedule.models.enums.EventStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class UserEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    public UserEventConsumer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(
            topics = "user-response-topic",
            groupId = "schedule-group",
            containerFactory = "listenerContainerFactory"
    )
    public void handleUserResponse(UserResponseEventDto event) {

        // Add this line immediately
        log.info("EVENT RECEIVED: Status={}, User={}", event.getEventTypeStatus(), event.getUserId());

        if (EventStatus.SUCCESS.equals(event.getEventTypeStatus())) {
            UUID userId = event.getUserId();

            Map<String, Object> map = new HashMap<>();
            map.put("name", event.getName());
            map.put("phoneNo", event.getPhoneNo());
            map.put("status", event.getStatus());
            map.put("role", event.getRole());
            if (("SUPERVISOR".equals(event.getRole()) || "DRIVER".equals(event.getRole())) && event.getTehsilId() != null) {
                map.put("tehsilId", event.getTehsilId().toString());
            } else {
                map.put("tehsilId", "");
            }
            if ("SUPERVISOR".equals(event.getRole()) && event.getYardId() != null) {
                map.put("yardId", event.getYardId().toString());
            } else {
                map.put("yardId", "");
            }
            log.info("Saving Data TO Redis");
            String redisKey = "wtms:user:" + userId;
            redisTemplate.opsForHash().putAll(redisKey, map);
        }
    }
}