package com.yasirkhan.schedule.services.implementation;

import com.yasirkhan.schedule.exceptions.DataBaseException;
import com.yasirkhan.schedule.exceptions.ResourceNotFoundException;
import com.yasirkhan.schedule.models.entities.Schedule;
import com.yasirkhan.schedule.models.entities.ShiftTemplate;
import com.yasirkhan.schedule.models.entities.Status;
import com.yasirkhan.schedule.repositories.ScheduleRepository;
import com.yasirkhan.schedule.repositories.ShiftTemplateRepository;
import com.yasirkhan.schedule.requests.ScheduleRequest;
import com.yasirkhan.schedule.responses.ScheduleResponse;
import com.yasirkhan.schedule.services.ScheduleService;
import com.yasirkhan.schedule.utils.ResponseConversion;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ScheduleRepository scheduleRepository;
    private final ShiftTemplateRepository templateRepository;

    public ScheduleServiceImpl(RedisTemplate<String, Object> redisTemplate, ScheduleRepository scheduleRepository, ShiftTemplateRepository templateRepository) {
        this.redisTemplate = redisTemplate;
        this.scheduleRepository = scheduleRepository;
        this.templateRepository = templateRepository;
    }

    @Override
    @Transactional
    public ScheduleResponse createSchedule(ScheduleRequest request) {

        // VALIDATION: Prevent Double Booking for Vehicles
        boolean isVehicleBooked = scheduleRepository.existsByVehicleNoAndScheduleDateAndTemplate_TemplateId(
                request.getVehicleNo(), request.getScheduleDate(), request.getTemplateId());

        if (isVehicleBooked) {
            throw new IllegalArgumentException("This Vehicle is already assigned to this shift on the selected date.");
        }

        // VALIDATION: Prevent Double Booking for Drivers
        boolean isDriverBooked = scheduleRepository.existsByDriverIdAndScheduleDateAndTemplate_TemplateId(
                request.getDriverId(), request.getScheduleDate(), request.getTemplateId());

        if (isDriverBooked) {
            throw new IllegalArgumentException("This Driver is already assigned to this shift on the selected date.");
        }

        Schedule schedule = new Schedule();
        schedule.setScheduleName(request.getScheduleName());
        schedule.setVehicleNo(request.getVehicleNo());
        schedule.setDriverId(request.getDriverId());
        schedule.setRouteId(request.getRouteId());
        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setShiftStatus(Status.ASSIGNED);

        // Creates a proxy object just to map the Foreign Key. Zero DB hits!
        ShiftTemplate templateProxy = templateRepository.getReferenceById(request.getTemplateId());

        // Assign the proxy to the schedule
        schedule.setTemplate(templateProxy);

        Schedule savedSchedule = scheduleRepository.save(schedule);

        // Use your static util to do the heavy lifting for DB fields
        ScheduleResponse response =
                ResponseConversion.toScheduleResponse(schedule);

        // Get Driver Data from Redis
        String userKey = "wtms:user:" + schedule.getDriverId();
        String driverName = (String) redisTemplate.opsForHash().get(userKey, "name");
        String driverPhone = (String) redisTemplate.opsForHash().get(userKey, "phoneNo");
        String driverStatus = (String) redisTemplate.opsForHash().get(userKey, "status");
        response.setName(driverName != null ? driverName : "Unknown Name");
        response.setPhoneNo(driverPhone != null ? driverPhone : "Unknown Phone");
        response.setDriverStatus(driverStatus != null ? driverStatus : "Unknown status");

        // Get Vehicle Data from Redis
        String vehicleKey = "wtms:vehicle:" + schedule.getVehicleNo();
        String vehicleStatus = (String) redisTemplate.opsForHash().get(vehicleKey, "status");
        response.setVehicleNo(schedule.getVehicleNo());
        response.setVehicleStatus(vehicleStatus != null ? vehicleStatus : "Unknown Status");

        // Get Route Data from Redis
        String routeKey = "wtms:route:" + schedule.getRouteId();
        String origin = (String) redisTemplate.opsForHash().get(routeKey, "origin");
        String destination = (String) redisTemplate.opsForHash().get(routeKey, "destination");
        response.setRouteOrigin(origin != null ? origin : "Unknown Origin");
        response.setRouteDestination(destination != null ? destination : "Unknown Destination");

        return response;
    }

    @Override
    @Transactional
    public void updateSchedule(Map<String, Object> updates) {
        UUID scheduleId = UUID.fromString(updates.get("scheduleId").toString());

        Schedule dbSchedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule with ID: " + scheduleId + " Not Found."));

        updates.forEach((key, value) -> {
            switch (key){
                case "scheduleName" -> dbSchedule.setScheduleName((String) value);
                case "vehicleId" -> dbSchedule.setVehicleNo(value.toString());
                case "driverId" -> dbSchedule.setDriverId(UUID.fromString(value.toString()));
                case "routeId" -> dbSchedule.setRouteId(UUID.fromString(value.toString()));
                case "scheduleDate" -> dbSchedule.setScheduleDate(LocalDate.parse(value.toString()));
                case "shiftStatus" -> dbSchedule.setShiftStatus(Status.valueOf(value.toString()));
            }
        });

        try {
            scheduleRepository.save(dbSchedule);
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
        // 1. Fetch raw schedules from PostgreSQL
        List<Schedule> dbSchedules = scheduleRepository.findAll();

        // 2. Map and Enrich
        return dbSchedules.stream().map(schedule -> {

            // Use your static util to do the heavy lifting for DB fields
            ScheduleResponse response = ResponseConversion.toScheduleResponse(schedule);

            // Get Driver Data from Redis
            String userKey = "wtms:user:" + schedule.getDriverId();
            String driverName = (String) redisTemplate.opsForHash().get(userKey, "name");
            String driverPhone = (String) redisTemplate.opsForHash().get(userKey, "phoneNo");
            String driverStatus = (String) redisTemplate.opsForHash().get(userKey, "status");
            response.setName(driverName != null ? driverName : "Unknown Name");
            response.setPhoneNo(driverPhone != null ? driverPhone : "Unknown Phone");
            response.setDriverStatus(driverStatus != null ? driverStatus : "Unknown status");

            // Get Vehicle Data from Redis
            String vehicleKey = "wtms:vehicle:" + schedule.getVehicleNo();
            String vehicleStatus = (String) redisTemplate.opsForHash().get(vehicleKey, "status");
            response.setVehicleNo(schedule.getVehicleNo());
            response.setVehicleStatus(vehicleStatus != null ? vehicleStatus : "Unknown Status");

            // Get Route Data from Redis
            String routeKey = "wtms:route:" + schedule.getRouteId();
            String origin = (String) redisTemplate.opsForHash().get(routeKey, "origin");
            String destination = (String) redisTemplate.opsForHash().get(routeKey, "destination");
            response.setRouteOrigin(origin != null ? origin : "Unknown Origin");
            response.setRouteDestination(destination != null ? destination : "Unknown Destination");
            return response;

        }).collect(Collectors.toList());
    }
}