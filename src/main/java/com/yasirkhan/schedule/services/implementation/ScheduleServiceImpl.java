package com.yasirkhan.schedule.services.implementation;

import com.yasirkhan.schedule.exceptions.DataBaseException;
import com.yasirkhan.schedule.exceptions.ResourceNotFoundException;
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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
            // Save to PostgreSQL
            Schedule savedSchedule = scheduleRepository.save(schedule);

            // Sync to local Redis Cache
            syncScheduleToRedis(savedSchedule);

            ScheduleResponse response = ResponseConversion.toScheduleResponse(savedSchedule);

            // Enrich with foreign Redis Data for the API response payload
            String userKey = "wtms:user:" + schedule.getDriverId();
            response.setDriverName((String) redisTemplate.opsForHash().get(userKey, "name"));
            response.setDriverPhoneNo((String) redisTemplate.opsForHash().get(userKey, "phoneNo"));
            response.setDriverStatus((String) redisTemplate.opsForHash().get(userKey, "status"));

            String vehicleKey = "wtms:vehicle:" + schedule.getVehicleNo();
            response.setVehicleStatus((String) redisTemplate.opsForHash().get(vehicleKey, "status"));

            String routeKey = "wtms:route:" + schedule.getRouteId();
            response.setRouteOrigin((String) redisTemplate.opsForHash().get(routeKey, "origin"));
            response.setRouteDestination((String) redisTemplate.opsForHash().get(routeKey, "destination"));

            // 4. Broadcast to Kafka
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

            // Sync the fully updated state to Redis
            syncScheduleToRedis(savedSchedule);

            ScheduleResponse response = ResponseConversion.toScheduleResponse(savedSchedule);
            publishScheduleEvent(EventType.UPDATE, EventStatus.SUCCESS, response);

        } catch (Exception e){
            throw new DataBaseException(e.getMessage());
        }
    }

    @Override
    public ScheduleResponse getScheduleById(UUID scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule with ID: " + scheduleId + " Not Found."));
        return ResponseConversion.toScheduleResponse(schedule);
    }

    @Override
    public List<ScheduleResponse> getAllSchedule() {
        List<Schedule> dbSchedules = scheduleRepository.findAll();

        return dbSchedules.stream().map(schedule -> {
            ScheduleResponse response = ResponseConversion.toScheduleResponse(schedule);

            String userKey = "wtms:user:" + schedule.getDriverId();
            String driverName = (String) redisTemplate.opsForHash().get(userKey, "name");
            String driverPhone = (String) redisTemplate.opsForHash().get(userKey, "phoneNo");
            String driverStatus = (String) redisTemplate.opsForHash().get(userKey, "status");
            response.setDriverName(driverName != null ? driverName : "Unknown Name");
            response.setDriverPhoneNo(driverPhone != null ? driverPhone : "Unknown Phone");
            response.setDriverStatus(driverStatus != null ? driverStatus : "Unknown status");

            String vehicleKey = "wtms:vehicle:" + schedule.getVehicleNo();
            String vehicleStatus = (String) redisTemplate.opsForHash().get(vehicleKey, "status");
            response.setVehicleNo(schedule.getVehicleNo());
            response.setVehicleStatus(vehicleStatus != null ? vehicleStatus : "Unknown Status");

            String routeKey = "wtms:route:" + schedule.getRouteId();
            String origin = (String) redisTemplate.opsForHash().get(routeKey, "origin");
            String destination = (String) redisTemplate.opsForHash().get(routeKey, "destination");
            response.setRouteOrigin(origin != null ? origin : "Unknown Origin");
            response.setRouteDestination(destination != null ? destination : "Unknown Destination");

            return response;

        }).collect(Collectors.toList());
    }

    // --- Redis Sync Helper (Schedule) ---
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

    // --- Kafka Publisher Helper ---
    private void publishScheduleEvent(EventType type, EventStatus status, ScheduleResponse response) {
        ScheduleResponseEventDto eventDto = ScheduleResponseEventDto.builder()
                .type(type)
                .eventTypeStatus(status)
                .scheduleData(response)
                .build();
        eventPublisher.publishEvent(eventDto);
    }
}