package com.yasirkhan.schedule.services.implementation;

import com.yasirkhan.schedule.models.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
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

        // Get the current Supervisor's Tehsil ID from the Security Context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Unauthorized: No valid session found.");
        }
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String supervisorId = principal.userId();
        String userKey = "wtms:user:" + supervisorId;
        String tehsilId = (String) redisTemplate.opsForHash().get(userKey, "tehsilId");

        if (tehsilId == null || tehsilId.isEmpty()) {
            throw new ResourceNotFoundException("No territory (Tehsil) assigned to this supervisor.");
        }

        // Filter Vehicles: Must be ACTIVE and belong to the Supervisor's Tehsil
        List<Map<Object, Object>> activeVehicles = getAllVehiclesFromCache().stream()
                .filter(v -> "ACTIVE".equals(v.get("status")) && tehsilId.equals(v.get("tehsilId")))
                .toList();

        // Filter Drivers: Must be ACTIVE and belong to the Supervisor's Tehsil
        List<Map<Object, Object>> activeDrivers = getAllDriversFromCache().stream()
                .filter(d -> "ACTIVE".equals(d.get("status")) && tehsilId.equals(d.get("tehsilId")))
                .toList();

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

        return AvailableAssetResponse.builder()
                .availableVehicles(finalValidVehicles.stream().map(v -> ResourceResponse.VehicleOption.builder()
                        .vehicleNo(v.get("vehicleNo").toString())
                        .status(v.get("status") != null ? v.get("status").toString() : "ACTIVE")
                        .build()).toList())
                .availableDrivers(finalValidDrivers.stream().map(d -> ResourceResponse.DriverOption.builder()
                        .driverId(UUID.fromString(d.get("driverId").toString()))
                        .name(d.get("name") != null ? d.get("name").toString() : "Unknown")
                        .phoneNo(d.get("phoneNo") != null ? d.get("phoneNo").toString() : null)
                        .status(d.get("status") != null ? d.get("status").toString() : "ACTIVE")
                        .build()).toList())
                .build();
    }

    // TODO: UPDATE THIS METHOD
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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Unauthorized: No valid session found.");
        }
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String supervisorId = principal.userId();
        String userKey = "wtms:user:" + supervisorId;
        String tehsilId = (String) redisTemplate.opsForHash().get(userKey, "tehsilId");

        if (tehsilId == null || tehsilId.isEmpty()) {
            throw new ResourceNotFoundException("No territory (Tehsil) assigned to this supervisor.");
        }

        List<Map<Object, Object>> activeRoutes = getActiveRoutesByTehsilFromCache(tehsilId);

        return AvailableResourceResponse.builder()
                .activeRoutes(activeRoutes.stream().map(r -> ResourceResponse.RouteOption.builder()
                        .routeId(UUID.fromString(r.get("routeId").toString()))
                        .routeName(r.get("routeName").toString())
                        .tehsilId(UUID.fromString(r.get("tehsilId").toString()))
                        .tehsilName(r.get("tehsilName").toString())
                        .build()).collect(Collectors.toList()))
                .activeTemplates(getAllActiveTemplatesFromCache().stream()
                        .map(t -> ResourceResponse.TemplateOption.builder()
                                .templateId(UUID.fromString(t.get("templateId").toString()))
                                .shiftName(t.get("shiftName").toString())
                                .startTime(t.get("startTime").toString())
                                .endTime(t.get("endTime").toString())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public List<Map<Object, Object>> getActiveYardsByTehsilFromCache(String tehsilId) {
        List<Map<Object, Object>> filteredYards = new ArrayList<>();

        Set<String> keys = redisTemplate.keys("wtms:yard:*");
        if (keys != null) {
            for (String key : keys) {
                Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

                String yardTehsilId = data.get("tehsilId") != null ? data.get("tehsilId").toString() : "";
                String status = data.get("status") != null ? data.get("status").toString() : "";

                // Only return if it's ACTIVE and belongs to the Supervisor's Tehsil
                if ("ACTIVE".equals(status) && tehsilId.equals(yardTehsilId)) {
                    data.put("yardId", key.replace("wtms:yard:", ""));
                    filteredYards.add(data);
                }
            }
        }
        return filteredYards;
    }

    public List<Map<Object, Object>> getActiveRoutesByTehsilFromCache(String tehsilId) {
        List<Map<Object, Object>> filteredRoutes = new ArrayList<>();

        Set<String> keys = redisTemplate.keys("wtms:route:*");

        if (keys != null) {
            for (String key : keys) {
                Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

                String routeTehsilId = data.get("tehsilId") != null ? data.get("tehsilId").toString() : "";
                String status = data.get("status") != null ? data.get("status").toString() : "";

                // Only return if it's ACTIVE and matches the Supervisor's Tehsil
                if ("ACTIVE".equals(status) && tehsilId.equals(routeTehsilId)) {
                    data.put("routeId", key.replace("wtms:route:", ""));
                    filteredRoutes.add(data);
                }
            }
        }
        return filteredRoutes;
    }

    // Helpers Methods
    private List<Map<Object, Object>> getAllActiveTemplatesFromCache() {
        return getAllTemplatesFromCache().stream()
                .filter(t -> "ACTIVE".equals(t.get("status")))
                .toList();
    }

    private List<Map<Object, Object>> getAllVehiclesFromCache() {
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

    private List<Map<Object, Object>> getAllDriversFromCache() {
        List<Map<Object, Object>> allDrivers = new ArrayList<>();
        Set<String> keys = redisTemplate.keys("wtms:user:*");
        if (keys != null) {
            for (String key : keys) {
                Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
                if ("DRIVER".equals(data.get("role"))) {
                    data.put("driverId", key.replace("wtms:user:", ""));
                    allDrivers.add(data);
                }
            }
        }
        return allDrivers;
    }

    private List<Map<Object, Object>> getAllTemplatesFromCache() {
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