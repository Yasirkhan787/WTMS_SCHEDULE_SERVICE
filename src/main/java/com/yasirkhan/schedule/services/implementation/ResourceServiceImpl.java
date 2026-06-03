package com.yasirkhan.schedule.services.implementation;

import com.yasirkhan.schedule.exceptions.ResourceNotFoundException;
import com.yasirkhan.schedule.models.entities.Schedule;
import com.yasirkhan.schedule.models.entities.ShiftTemplate;
import com.yasirkhan.schedule.repositories.ScheduleRepository;
import com.yasirkhan.schedule.repositories.ShiftTemplateRepository;
import com.yasirkhan.schedule.requests.AvailableAssetRequest;
import com.yasirkhan.schedule.responses.AvailableAssetResponse;
import com.yasirkhan.schedule.responses.AvailableResourceResponse;
import com.yasirkhan.schedule.responses.ResourceResponse;
import com.yasirkhan.schedule.services.ResourceService;
import com.yasirkhan.schedule.utils.ResponseConversion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ResourceServiceImpl implements ResourceService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ScheduleRepository scheduleRepository;
    private final ShiftTemplateRepository templateRepository;

    public ResourceServiceImpl(RedisTemplate<String, Object> redisTemplate, ScheduleRepository scheduleRepository, ShiftTemplateRepository templateRepository) {
        this.redisTemplate = redisTemplate;
        this.scheduleRepository = scheduleRepository;
        this.templateRepository = templateRepository;
    }

    // Drivers And Vehicles
    @Override
    public AvailableAssetResponse getAvailableAssets(AvailableAssetRequest request) {

        // THE BASE QUERY (Status Check via Redis)
        List<Map<Object, Object>> activeVehicles = getAllActiveVehiclesFromCache();

        List<Map<Object, Object>> activeDrivers = getAllActiveDriversFromCache();

        // THE DOUBLE-BOOKING CHECK (Time Conflict)
        List<Schedule> dailySchedules = scheduleRepository.findByScheduleDate(request.getScheduleDate());

        List<Map<Object, Object>> timeFilteredVehicles = activeVehicles.stream()
                .filter(vehicle -> dailySchedules.stream()
                        .noneMatch(schedule -> schedule.getVehicleNo().equals(vehicle.get("vehicleNo").toString())
                                && schedule.getTemplate().getTemplateId().equals(request.getShiftTemplateId())))
                .toList();

        List<Map<Object, Object>> timeFilteredDrivers = activeDrivers.stream()
                .filter(driver -> dailySchedules.stream()
                        .noneMatch(schedule -> schedule.getDriverId().toString().equals(driver.get("driverId").toString())
                                && schedule.getTemplate().getTemplateId().equals(request.getShiftTemplateId())))
                .toList();

        // THE GEOGRAPHICAL CHAIN CHECK (Teleportation Blocker)
        Map<Object, Object> targetRoute = getRouteFromCache(request.getRouteId());
        ShiftTemplate targetShift = templateRepository.findById(request.getShiftTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template Not Found"));

        List<Map<Object, Object>> finalValidVehicles = timeFilteredVehicles.stream()
                .filter(vehicle -> isGeographicallyValid(vehicle.get("vehicleNo").toString(), targetRoute, targetShift, dailySchedules))
                .toList();

        List<Map<Object, Object>> finalValidDrivers = timeFilteredDrivers.stream()
                .filter(driver -> isGeographicallyValid(driver.get("driverId").toString(), targetRoute, targetShift, dailySchedules))
                .toList();

        // Package and send to frontend
        return AvailableAssetResponse.builder()
                .availableVehicles(finalValidVehicles.stream().map(v -> ResourceResponse.VehicleOption.builder()
                        .vehicleNo(v.get("vehicleNo").toString())
                        .status(v.get("status") != null ? v.get("status").toString() : "ACTIVE")
                        .build()).toList())
                .availableDrivers(finalValidDrivers.stream().map(d -> ResourceResponse.DriverOption.builder()
                        .driverId(UUID.fromString(d.get("driverId").toString()))
                        .name(d.get("name") != null ? d.get("name").toString() : "Unknown")
                        .status(d.get("status") != null ? d.get("status").toString() : "ACTIVE")
                        .build()).toList())
                .build();
    }

    private boolean isGeographicallyValid(String assetIdentifier, Map<Object, Object> targetRoute, ShiftTemplate targetShift, List<Schedule> dailySchedules) {

        Schedule precedingSchedule = dailySchedules.stream()
                .filter(s -> s.getVehicleNo().equals(assetIdentifier) || s.getDriverId().toString().equals(assetIdentifier))
                .filter(s -> s.getTemplate().getEndTime().isBefore(targetShift.getStartTime()) ||
                        s.getTemplate().getEndTime().equals(targetShift.getStartTime()))
                .max(Comparator.comparing(s -> s.getTemplate().getEndTime()))
                .orElse(null);

        if (precedingSchedule == null) {
            return true;
        }

        Map<Object, Object> precedingRoute = getRouteFromCache(precedingSchedule.getRouteId());

        String previousDestination = precedingRoute.get("destination") != null ? precedingRoute.get("destination").toString() : "";
        String newOrigin = targetRoute.get("origin") != null ? targetRoute.get("origin").toString() : "";

        if (previousDestination.isEmpty() || newOrigin.isEmpty()) {
            return true;
        }

        return previousDestination.equalsIgnoreCase(newOrigin);
    }

    private Map<Object, Object> getRouteFromCache(UUID routeId) {
        Map<Object, Object> routeData = redisTemplate.opsForHash().entries("wtms:route:" + routeId);
        if (routeData.isEmpty()) {
            throw new ResourceNotFoundException("Route data not found in cache for ID: " + routeId);
        }
        return routeData;
    }

    @Override
    public AvailableResourceResponse getAvailableResources() {

        List<Map<Object, Object>> activeRoutes = getAllRoutesFromCache().stream()
                .filter(r -> "ACTIVE".equals(r.get("status")))
                .toList();

        List<Map<Object, Object>> activeTemplates = getAllTemplatesFromCache().stream()
                .filter(t -> "ACTIVE".equals(t.get("status")))
                .toList();

        return AvailableResourceResponse.builder()
                .activeRoutes(activeRoutes.stream().map(r -> ResourceResponse.RouteOption.builder()
                        .routeId(UUID.fromString(r.get("routeId").toString()))
                        .origin(r.get("origin") != null ? r.get("origin").toString() : "")
                        .destination(r.get("destination") != null ? r.get("destination").toString() : "")
                        .status(r.get("status") != null ? r.get("status").toString() : "ACTIVE")
                        .build()).toList())

                .activeTemplates(activeTemplates.stream().map(t -> ResourceResponse.TemplateOption.builder()
                        .templateId(UUID.fromString(t.get("templateId").toString()))
                        .shiftName(t.get("shiftName") != null ? t.get("shiftName").toString() : "")
                        .startTime(t.get("startTime") != null ? java.time.LocalTime.parse(t.get("startTime").toString()) : null)
                        .endTime(t.get("endTime") != null ? java.time.LocalTime.parse(t.get("endTime").toString()) : null)
                        .status(t.get("status") != null ? t.get("status").toString() : "ACTIVE")
                        .build()).toList())
                .build();
    }

    @Override
    public ResourceResponse getAllResources() {
        List<Map<Object, Object>> activeVehicles = getAllActiveVehiclesFromCache();

        List<Map<Object, Object>> activeDrivers = getAllActiveDriversFromCache();

        List<Map<Object, Object>> activeRoutes = getAllActiveRoutesFromCache();

        List<Map<Object, Object>> activeTemplates = getAllActiveTemplatesFromCache();

        return ResponseConversion.toResourceResponse(activeVehicles, activeDrivers, activeRoutes, activeTemplates);
    }

    // Helpers Methods

    // Get All Active Vehicles from Redis Cache
    public List<Map<Object, Object>> getAllActiveVehiclesFromCache() {
        return getAllVehiclesFromCache().stream()
                .filter(v -> "ACTIVE".equals(v.get("status")))
                .toList();
    }

    // Get All Active Drivers from Redis Cache
    public List<Map<Object, Object>> getAllActiveDriversFromCache() {
        return getAllDriversFromCache().stream()
                .filter(d -> "ACTIVE".equals(d.get("status")))
                .toList();
    }
    // Get All Active Routes from Redis Cache
    public List<Map<Object, Object>> getAllActiveRoutesFromCache() {
        return getAllRoutesFromCache().stream()
                .filter(r -> "ACTIVE".equals(r.get("status")))
                .toList();
    }
    // Get All Active Templates from Redis Cache
    public List<Map<Object, Object>> getAllActiveTemplatesFromCache() {
        return getAllTemplatesFromCache().stream()
                .filter(t -> "ACTIVE".equals(t.get("status")))
                .toList();
    }

    // Get All Vehicles from Redis Cache
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

    // Get All Drivers from Redis Cache
    public List<Map<Object, Object>> getAllDriversFromCache() {
        List<Map<Object, Object>> allDrivers = new ArrayList<>();
        Set<String> keys = redisTemplate.keys("wtms:user:*");
        if (keys != null) {
            for (String key : keys) {
                Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

                // Only add to list if they are actually a DRIVER!
                if ("DRIVER".equals(data.get("role"))) {
                    data.put("driverId", key.replace("wtms:user:", ""));
                    allDrivers.add(data);
                }
            }
        }
        return allDrivers;
    }

    // Get All Routes from Redis Cache
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

    // Get All Templates from Redis Cache
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