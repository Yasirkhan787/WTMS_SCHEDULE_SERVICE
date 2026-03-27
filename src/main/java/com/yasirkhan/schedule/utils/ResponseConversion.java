package com.yasirkhan.schedule.utils;

import com.yasirkhan.schedule.models.entities.Schedule;
import com.yasirkhan.schedule.responses.ScheduleResponse;

public class ResponseConversion {


    public static ScheduleResponse toScheduleResponse(Schedule savedSchedule) {

        return
                ScheduleResponse
                        .builder()
                        .scheduleId(savedSchedule.getScheduleId())
                        .scheduleName(savedSchedule.getScheduleName())
                        .vehicleId(savedSchedule.getVehicleId())
                        .driverId(savedSchedule.getDriverId())
                        .routeId(savedSchedule.getRouteId())
                        .shift(savedSchedule.getShift())
                        .scheduleTime(savedSchedule.getScheduleTime())
                        .build();
    }
}
