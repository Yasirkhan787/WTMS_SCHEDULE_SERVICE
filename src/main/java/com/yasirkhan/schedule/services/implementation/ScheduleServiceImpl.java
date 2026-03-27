package com.yasirkhan.schedule.services.implementation;

import com.yasirkhan.schedule.exceptions.DataBaseException;
import com.yasirkhan.schedule.exceptions.ResourceNotFoundException;
import com.yasirkhan.schedule.models.entities.Schedule;
import com.yasirkhan.schedule.models.entities.Shift;
import com.yasirkhan.schedule.repositories.ScheduleRepository;
import com.yasirkhan.schedule.requests.ScheduleRequest;
import com.yasirkhan.schedule.responses.ScheduleResponse;
import com.yasirkhan.schedule.services.ScheduleService;
import com.yasirkhan.schedule.utils.ResponseConversion;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public ScheduleServiceImpl(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    @Transactional
    public ScheduleResponse createSchedule(ScheduleRequest request) {

        Schedule schedule = new Schedule();
        schedule.setScheduleName(request.getScheduleName());
        schedule.setVehicleId(request.getVehicleId());
        schedule.setDriverId(request.getDriverId());
        schedule.setRouteId(request.getRouteId());
        schedule.setShift(request.getShift());
        schedule.setScheduleTime(request.getScheduleTime());

        Schedule savedSchedule = null;

        try {
            savedSchedule = scheduleRepository.save(schedule);
        }catch (Exception e){
            throw new DataBaseException(e.getMessage());
        }
        return ResponseConversion
                .toScheduleResponse(savedSchedule);
    }

    @Override
    @Transactional
    public void updateSchedule(Map<String, Object> updates) {

        UUID scheduleId = UUID.fromString(updates.get("scheduleId").toString());

        Schedule dbSchedule =
                scheduleRepository
                        .findById(scheduleId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Schedule with ID: " + scheduleId + "Not Fount."));

        updates.forEach((key, value) -> {
            switch (key){
                case "scheduleName": dbSchedule.setScheduleName((String) value);
                case "vehicleId": dbSchedule.setVehicleId(UUID.fromString(value.toString()));
                case "driverId": dbSchedule.setDriverId(UUID.fromString(value.toString()));
                case "routeId": dbSchedule.setRouteId(UUID.fromString(value.toString()));
                case "shift": dbSchedule.setShift(Shift.valueOf(value.toString()));
                case "scheduleTime" : dbSchedule.setScheduleTime((LocalDateTime) value);
            }
        });

        try {
            scheduleRepository.save(dbSchedule);
        }catch (Exception e){
            throw new DataBaseException(e.getMessage());
        }
    }

    @Override
    public ScheduleResponse getScheduleById(UUID scheduleId) {

        Schedule schedule =
                scheduleRepository
                        .findById(scheduleId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Schedule with ID: " + scheduleId + "Not Fount."));

        return ResponseConversion
                .toScheduleResponse(schedule);
    }

    @Override
    public List<ScheduleResponse> getAllSchedule() {

        List<Schedule> schedules =
                scheduleRepository.findAll();

        if (schedules.isEmpty()) {
            throw new  ResourceNotFoundException("No schedules found in Database");
        }

        return schedules
                .stream()
                .map(ResponseConversion::toScheduleResponse)
                .collect(Collectors.toList());
    }
}
