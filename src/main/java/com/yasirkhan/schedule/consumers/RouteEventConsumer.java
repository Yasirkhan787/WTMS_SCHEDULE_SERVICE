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

            if (routeId == null) {
                return;
            }

            Map<String, Object> map = new HashMap<>();

            map.put("routeName", event.getRouteName());
            map.put("path", event.getRoutePath());
            map.put("tehsilId", event.getTehsilId() != null ? event.getTehsilId().toString() : null);
            map.put("tehsilName", event.getTehsilName());
            map.put("sourceYardId", event.getSourceYardId() != null ? event.getSourceYardId().toString() : null);
            map.put("sourceYardName", event.getSourceYardName());
            map.put("sourceYardType", event.getSourceYardType());
            map.put("destinationYardId", event.getDestinationYardId() != null ? event.getDestinationYardId().toString() : null);
            map.put("destinationYardName", event.getDestinationYardName());
            map.put("destinationYardType", event.getDestinationYardType());
            map.put("status", event.getStatus());

            if (event.getYardData() != null) {
                RouteResponseEventDto.YardResponse yard = event.getYardData();
                map.put("destinationYardBoundaryType", yard.getBoundaryType());
                map.put("destinationYardRadiusMeters", yard.getRadiusMeters()  != null ? yard.getRadiusMeters() : null);
                map.put("destinationYardPolygonPath", yard.getPolygonPath() != null ? yard.getPolygonPath() : null);

                if (yard.getCenterCoords() != null) {
                    map.put("destinationYardCenterLat", yard.getCenterCoords().getLat());
                    map.put("destinationYardCenterLng", yard.getCenterCoords().getLng());
                }
            }

            String redisKey = "wtms:route:" + routeId;
            redisTemplate.opsForHash().putAll(redisKey, map);
        }
    }
}