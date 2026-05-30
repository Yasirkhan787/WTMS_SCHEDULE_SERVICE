package com.yasirkhan.schedule.utils;

import com.yasirkhan.schedule.models.entities.Schedule;
import com.yasirkhan.schedule.models.entities.ShiftTemplate;
import com.yasirkhan.schedule.responses.ResourceResponse;
import com.yasirkhan.schedule.responses.ScheduleResponse;
import com.yasirkhan.schedule.responses.ShiftTemplateResponse;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ResponseConversion {

    public static ScheduleResponse toScheduleResponse(Schedule savedSchedule) {

        ScheduleResponse response =
                ScheduleResponse
                        .builder()
                        .scheduleId(savedSchedule.getScheduleId())
                        .scheduleName(savedSchedule.getScheduleName())
                        .scheduleDate(savedSchedule.getScheduleDate())
                        .shiftStatus(savedSchedule.getShiftStatus())
                        .build();


        if (savedSchedule.getTemplate() != null) {
            response.setShiftName(savedSchedule.getTemplate().getShiftName());
            response.setStartTime(savedSchedule.getTemplate().getStartTime());
            response.setEndTime(savedSchedule.getTemplate().getEndTime());
        }

        return response;
    }

    public static ShiftTemplateResponse toShiftTemplateResponse(ShiftTemplate savedShiftTemplate) {

        return
                ShiftTemplateResponse
                        .builder()
                        .templateId(savedShiftTemplate.getTemplateId())
                        .shiftName(savedShiftTemplate.getShiftName())
                        .startTime(savedShiftTemplate.getStartTime())
                        .endTime(savedShiftTemplate.getEndTime())
                        .remarks(savedShiftTemplate.getRemarks())
                        .build();
    }

    public static ResourceResponse toResourceResponse(
            List<Map<Object, Object>> activeVehicles,
            List<Map<Object, Object>> activeDrivers,
            List<Map<Object, Object>> activeRoutes,
            List<Map<Object, Object>> activeTemplates) {

        return ResourceResponse.builder()
                .vehicles(activeVehicles.stream().map(v -> ResourceResponse.VehicleOption.builder()
                        .vehicleNo(v.get("vehicleNo").toString())
                        .status(v.get("status").toString())
                        .build()).collect(Collectors.toList()))

                .drivers(activeDrivers.stream().map(d -> ResourceResponse.DriverOption.builder()
                        .driverId(UUID.fromString(d.get("driverId").toString()))
                        .name(d.get("name").toString())
                        .phoneNo(d.get("phoneNo").toString())
                        .status(d.get("status").toString())
                        .build()).collect(Collectors.toList()))

                .routes(activeRoutes.stream().map(r -> ResourceResponse.RouteOption.builder()
                        .routeId(UUID.fromString(r.get("routeId").toString()))
                        .origin(r.get("origin").toString())
                        .destination(r.get("destination").toString())
                        .status(r.get("status").toString())
                        .build()).collect(Collectors.toList()))

                .templates(activeTemplates.stream().map(t -> ResourceResponse.TemplateOption.builder()
                        .templateId(UUID.fromString(t.get("templateId").toString()))
                        .shiftName(t.get("shiftName").toString())
                        .startTime(LocalTime.parse(t.get("startTime").toString())) // Handle parsing safely
                        .endTime(LocalTime.parse(t.get("endTime").toString()))
                        .status(t.get("status").toString())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}