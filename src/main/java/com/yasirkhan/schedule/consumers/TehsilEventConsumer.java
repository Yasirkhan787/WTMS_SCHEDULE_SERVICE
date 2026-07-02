package com.yasirkhan.schedule.consumers;

import com.yasirkhan.schedule.models.dtos.TehsilResponseEventDto;
import com.yasirkhan.schedule.models.enums.EventStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class TehsilEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    public TehsilEventConsumer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(
            topics = "tehsil-response-topic",
            groupId = "schedule-group",
            containerFactory = "listenerContainerFactory"
    )
    public void handleYardResponse(TehsilResponseEventDto event) {

        if (EventStatus.SUCCESS.equals(event.getEventTypeStatus())) {
            UUID tehsilId = event.getTehsilId();

            Map<String, Object> map = new HashMap<>();
            map.put("tehsilName",event.getTehsilName());
            map.put("status",event.getStatus());

            String redisKey = "wtms:tehsil:" + tehsilId;
            redisTemplate.opsForHash().putAll(redisKey, map);
        }
    }
}