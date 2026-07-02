package com.yasirkhan.schedule.services.implementation;

import com.yasirkhan.schedule.exceptions.DataBaseException;
import com.yasirkhan.schedule.exceptions.ResourceNotFoundException;
import com.yasirkhan.schedule.exceptions.UnauthorizedException;
import com.yasirkhan.schedule.models.UserPrincipal;
import com.yasirkhan.schedule.models.dtos.ScheduleResponseEventDto;
import com.yasirkhan.schedule.models.entities.Schedule;
import com.yasirkhan.schedule.models.entities.ShiftTemplate;
import com.yasirkhan.schedule.models.enums.Status;
import com.yasirkhan.schedule.models.enums.EventStatus;
import com.yasirkhan.schedule.models.enums.EventType;
import com.yasirkhan.schedule.repositories.ScheduleRepository;
import com.yasirkhan.schedule.repositories.ShiftTemplateRepository;
import com.yasirkhan.schedule.requests.ScheduleRequest;
import com.yasirkhan.schedule.responses.ScheduleResponse;
import com.yasirkhan.schedule.services.ScheduleService;
import com.yasirkhan.schedule.utils.ResponseConversion;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ScheduleRepository scheduleRepository;
    private final ShiftTemplateRepository templateRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ScheduleServiceImpl(RedisTemplate<String, Object> redisTemplate,
                               ScheduleRepository scheduleRepository,
                               ShiftTemplateRepository templateRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.redisTemplate = redisTemplate;
        this.scheduleRepository = scheduleRepository;
        this.templateRepository = templateRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ScheduleResponse createSchedule(ScheduleRequest request) {

        boolean isVehicleBooked = scheduleRepository.existsByVehicleNoAndScheduleDateAndTemplate_TemplateId(
                request.getVehicleNo(), request.getScheduleDate(), request.getTemplateId());

        if (isVehicleBooked) {
            throw new IllegalArgumentException("Conflict: Vehicle " + request.getVehicleNo() + " was just booked by another user for this shift.");
        }

        boolean isDriverBooked = scheduleRepository.existsByDriverIdAndScheduleDateAndTemplate_TemplateId(
                request.getDriverId(), request.getScheduleDate(), request.getTemplateId());

        if (isDriverBooked) {
            throw new IllegalArgumentException("Conflict: This driver was just booked by another user for this shift.");
        }

        Schedule schedule = new Schedule();
        schedule.setScheduleName(request.getScheduleName());
        schedule.setVehicleNo(request.getVehicleNo());
        schedule.setDriverId(request.getDriverId());
        schedule.setRouteId(request.getRouteId());
        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setStatus(Status.ASSIGNED);

        ShiftTemplate templateProxy = templateRepository.getReferenceById(request.getTemplateId());
        schedule.setTemplate(templateProxy);

        try {
            Schedule savedSchedule = scheduleRepository.save(schedule);

            syncScheduleToRedis(savedSchedule);

            ScheduleResponse response = ResponseConversion.toScheduleResponse(savedSchedule);

            // Enrich with Redis Data
            enrichResponseFromRedis(savedSchedule, response, new HashMap<>());

            publishScheduleEvent(EventType.CREATE, EventStatus.SUCCESS, response);

            return response;

        } catch (Exception e) {
            throw new DataBaseException("Failed to save Schedule: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateSchedule(Map<String, Object> updates) {
        UUID scheduleId = UUID.fromString(updates.get("scheduleId").toString());

        Schedule dbSchedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule with ID: " + scheduleId + " Not Found."));

        updates.forEach((key, value) -> {
            if (value != null) {
                switch (key){
                    case "scheduleName" -> dbSchedule.setScheduleName((String) value);
                    case "vehicleId", "vehicleNo" -> dbSchedule.setVehicleNo(value.toString());
                    case "driverId" -> dbSchedule.setDriverId(UUID.fromString(value.toString()));
                    case "routeId" -> dbSchedule.setRouteId(UUID.fromString(value.toString()));
                    case "scheduleDate" -> dbSchedule.setScheduleDate(LocalDate.parse(value.toString()));
                    case "shiftStatus" -> dbSchedule.setStatus(Status.valueOf(value.toString()));
                }
            }
        });

        try {
            Schedule savedSchedule = scheduleRepository.save(dbSchedule);

            syncScheduleToRedis(savedSchedule);

            ScheduleResponse response = ResponseConversion.toScheduleResponse(savedSchedule);
            enrichResponseFromRedis(savedSchedule, response, new HashMap<>());
            publishScheduleEvent(EventType.UPDATE, EventStatus.SUCCESS, response);

        } catch (Exception e){
            throw new DataBaseException(e.getMessage());
        }
    }

    @Override
    public ScheduleResponse getScheduleById(UUID scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule with ID: " + scheduleId + " Not Found."));

        ScheduleResponse response = ResponseConversion.toScheduleResponse(schedule);
        enrichResponseFromRedis(schedule, response, new HashMap<>());
        return response;
    }

    @Override
    public List<ScheduleResponse> getAllSchedules(String statusFilter) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Unauthorized: No valid session found.");
        }
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String userId = principal.userId();
        String role = principal.role();

        List<Schedule> dbSchedules;

        if ("ADMIN".equals(role)) {
            dbSchedules = scheduleRepository.findAll();
        } else if ("DRIVER".equals(role)) {
            dbSchedules = scheduleRepository.findByDriverId(UUID.fromString(userId));
        } else if ("SUPERVISOR".equals(role)) {
            String tehsilId = (String) redisTemplate.opsForHash().get("wtms:user:" + userId, "tehsilId");
            if (tehsilId == null || tehsilId.isEmpty()) {
                throw new ResourceNotFoundException("No territory assigned to this supervisor.");
            }

            List<UUID> routeIds = new ArrayList<>();
            Set<String> routeKeys = redisTemplate.keys("wtms:route:*");

            if (routeKeys != null && !routeKeys.isEmpty()) {
                for (String key : routeKeys) {
                    String cachedTehsilId = (String) redisTemplate.opsForHash().get(key, "tehsilId");
                    if (tehsilId.equals(cachedTehsilId)) {
                        String routeIdStr = key.replace("wtms:route:", "");
                        routeIds.add(UUID.fromString(routeIdStr));
                    }
                }
            }

            if (routeIds.isEmpty()) {
                return Collections.emptyList();
            }

            dbSchedules = scheduleRepository.findByRouteIdIn(routeIds);

        } else {
            throw new UnauthorizedException("You do not have permission to view schedules.");
        }

        Map<String, Map<Object, Object>> localCache = new HashMap<>();

        return dbSchedules.stream()
                .filter(schedule -> statusFilter == null || schedule.getStatus().name().equalsIgnoreCase(statusFilter))
                .sorted(Comparator.comparing(Schedule::getScheduleDate))
                .map(schedule -> {
                    ScheduleResponse response = ResponseConversion.toScheduleResponse(schedule);

                    // Use helper method mapped with local cache for efficiency
                    enrichResponseFromRedis(schedule, response, localCache);

                    if (schedule.getTemplate() != null) {
                        response.setTemplateId(schedule.getTemplate().getTemplateId());
                    }

                    return response;
                }).collect(Collectors.toList());
    }

    @Override
    public ScheduleResponse findActiveScheduleForTrip(String vehicleNo, LocalDate date, LocalTime actualTime) {

        List<Schedule> dailySchedules = scheduleRepository.findByVehicleNoAndScheduleDateAndStatus(
                vehicleNo, date, Status.ASSIGNED);

        Schedule matchedSchedule = dailySchedules.stream()
                .filter(schedule -> {
                    LocalTime shiftStart = schedule.getTemplate().getStartTime();
                    LocalTime shiftEnd = schedule.getTemplate().getEndTime();

                    LocalTime bufferedStart = shiftStart.minusMinutes(60);
                    LocalTime bufferedEnd = shiftEnd.plusMinutes(60);

                    boolean isOvernightShift = bufferedStart.isAfter(bufferedEnd);

                    if (isOvernightShift) {
                        return actualTime.isAfter(bufferedStart) || actualTime.isBefore(bufferedEnd);
                    } else {
                        return actualTime.isAfter(bufferedStart) && actualTime.isBefore(bufferedEnd);
                    }
                })
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No schedule found within the buffered time window"));

        ScheduleResponse response = ResponseConversion.toScheduleResponse(matchedSchedule);
        enrichResponseFromRedis(matchedSchedule, response, new HashMap<>());
        return response;
    }


    // ==========================================
    //            PRIVATE HELPERS
    // ==========================================

    private void enrichResponseFromRedis(Schedule schedule, ScheduleResponse response, Map<String, Map<Object, Object>> cache) {
        // 1. Driver Data
        if (schedule.getDriverId() != null) {
            String userKey = "wtms:user:" + schedule.getDriverId();
            Map<Object, Object> driverData = cache.computeIfAbsent(userKey, k -> redisTemplate.opsForHash().entries(k));
            response.setDriverId(schedule.getDriverId());
            response.setDriverName(driverData.get("name") != null ? driverData.get("name").toString() : "Unknown Name");
            response.setDriverPhoneNo(driverData.get("phoneNo") != null ? driverData.get("phoneNo").toString() : "Unknown Phone");
            response.setDriverStatus(driverData.get("status") != null ? driverData.get("status").toString() : "Unknown status");
        }

        // 2. Vehicle Data
        if (schedule.getVehicleNo() != null) {
            String vehicleKey = "wtms:vehicle:" + schedule.getVehicleNo();
            Map<Object, Object> vehicleData = cache.computeIfAbsent(vehicleKey, k -> redisTemplate.opsForHash().entries(k));
            response.setVehicleNo(schedule.getVehicleNo());
            response.setVehicleStatus(vehicleData.get("status") != null ? vehicleData.get("status").toString() : "Unknown Status");
        }

        // 3. Route & Destination Yard Data
        if (schedule.getRouteId() != null) {
            String routeKey = "wtms:route:" + schedule.getRouteId();
            Map<Object, Object> routeData = cache.computeIfAbsent(routeKey, k -> redisTemplate.opsForHash().entries(k));

            // Core Route
            response.setRouteId(schedule.getRouteId());
            response.setRoutePath(routeData.get("path") != null ? routeData.get("path").toString() : "Unknown Route Path");
            response.setRouteName(routeData.get("routeName") != null ? routeData.get("routeName").toString() : "Unknown Route");
            response.setTehsilName(routeData.get("tehsilName") != null ? routeData.get("tehsilName").toString() : "Unknown Tehsil");

            Object tehsilIdObj = routeData.get("tehsilId");
            if (tehsilIdObj != null && !tehsilIdObj.toString().isEmpty()) {
                response.setTehsilId(UUID.fromString(tehsilIdObj.toString()));
            }

            // Destination Yard Fields
            response.setDestinationYardId(routeData.get("destinationYardId") != null ? routeData.get("destinationYardId").toString() : null);
            response.setDestinationYardName(routeData.get("destinationYardName") != null ? routeData.get("destinationYardName").toString() : null);
            response.setDestinationYardType(routeData.get("destinationYardType") != null ? routeData.get("destinationYardType").toString() : null);
            response.setDestinationYardBoundaryType(routeData.get("destinationYardBoundaryType") != null ? routeData.get("destinationYardBoundaryType").toString() : null);
            response.setDestinationYardRadiusMeters(routeData.get("destinationYardRadiusMeters") != null ? routeData.get("destinationYardRadiusMeters").toString() : null);
            response.setDestinationYardPolygonPath(routeData.get("destinationYardPolygonPath") != null ? routeData.get("destinationYardPolygonPath").toString() : null);
            response.setDestinationYardCenterLat(routeData.get("destinationYardCenterLat") != null ? routeData.get("destinationYardCenterLat").toString() : null);
            response.setDestinationYardCenterLng(routeData.get("destinationYardCenterLng") != null ? routeData.get("destinationYardCenterLng").toString() : null);
        }
    }

    private void syncScheduleToRedis(Schedule schedule) {
        String redisKey = "wtms:schedule:" + schedule.getScheduleId().toString();
        Map<String, Object> data = new HashMap<>();

        data.put("scheduleName", schedule.getScheduleName());
        data.put("vehicleNo", schedule.getVehicleNo());
        data.put("driverId", schedule.getDriverId() != null ? schedule.getDriverId().toString() : "");
        data.put("routeId", schedule.getRouteId() != null ? schedule.getRouteId().toString() : "");
        data.put("scheduleDate", schedule.getScheduleDate() != null ? schedule.getScheduleDate().toString() : "");
        data.put("templateId", schedule.getTemplate() != null ? schedule.getTemplate().getTemplateId().toString() : "");
        data.put("status", schedule.getStatus().name());

        redisTemplate.opsForHash().putAll(redisKey, data);
    }

    private void publishScheduleEvent(EventType type, EventStatus status, ScheduleResponse response) {
        ScheduleResponseEventDto eventDto = ScheduleResponseEventDto.builder()
                .type(type)
                .eventTypeStatus(status)
                .scheduleData(response)
                .build();
        eventPublisher.publishEvent(eventDto);
    }
}