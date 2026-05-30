package com.yasirkhan.schedule.services.implementation;

import com.yasirkhan.schedule.responses.ResourceResponse;
import com.yasirkhan.schedule.services.ResourceService;
import com.yasirkhan.schedule.utils.ResponseConversion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResourceServiceImpl implements ResourceService {

    private final RedisTemplate<String, Object> redisTemplate;

    public ResourceServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ResourceResponse getAllResources() {
        List<Map<Object, Object>> activeVehicles = getAllVehiclesFromCache().stream()
                .filter(v -> "ACTIVE".equals(v.get("status")))
                .toList();

        List<Map<Object, Object>> activeDrivers = getAllDriversFromCache().stream()
                .filter(d -> "ACTIVE".equals(d.get("status")))
                .toList();

        List<Map<Object, Object>> activeRoutes = getAllRoutesFromCache().stream()
                .filter(r -> "ACTIVE".equals(r.get("status")))
                .toList();

        List<Map<Object, Object>> activeTemplates = getAllTemplatesFromCache().stream()
                .filter(t -> "ACTIVE".equals(t.get("status")))
                .toList();

        return ResponseConversion.toResourceResponse(activeVehicles, activeDrivers, activeRoutes, activeTemplates);
    }

    public List<Map<Object, Object>> getAllVehiclesFromCache() {
        List<Map<Object, Object>> allVehicles = new ArrayList<>();
        Set<String> keys = redisTemplate.keys("wtms:vehicle:*");
        if (keys != null) {
            for (String key : keys) {
                Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
                data.put("vehicleNo", key.replace("wtms:vehicle:", ""));
                allVehicles.add(data);
            }
        }
        return allVehicles;
    }

    public List<Map<Object, Object>> getAllDriversFromCache() {
        List<Map<Object, Object>> allDrivers = new ArrayList<>();
        Set<String> keys = redisTemplate.keys("wtms:user:*");
        if (keys != null) {
            for (String key : keys) {
                Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
                data.put("driverId", key.replace("wtms:user:", ""));
                allDrivers.add(data);
            }
        }
        return allDrivers;
    }

    public List<Map<Object, Object>> getAllRoutesFromCache() {
        List<Map<Object, Object>> allRoutes = new ArrayList<>();
        Set<String> keys = redisTemplate.keys("wtms:route:*");
        if (keys != null) {
            for (String key : keys) {
                Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
                data.put("routeId", key.replace("wtms:route:", ""));
                allRoutes.add(data);
            }
        }
        return allRoutes;
    }

    public List<Map<Object, Object>> getAllTemplatesFromCache() {
        List<Map<Object, Object>> allTemplates = new ArrayList<>();
        Set<String> keys = redisTemplate.keys("wtms:template:*");
        if (keys != null) {
            for (String key : keys) {
                Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
                data.put("templateId", key.replace("wtms:template:", ""));
                allTemplates.add(data);
            }
        }
        return allTemplates;
    }
}