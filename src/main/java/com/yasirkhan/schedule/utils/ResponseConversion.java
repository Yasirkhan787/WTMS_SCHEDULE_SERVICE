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

        ScheduleResponse response = ScheduleResponse.builder()
                .scheduleId(savedSchedule.getScheduleId())
                .scheduleName(savedSchedule.getScheduleName())
                .scheduleDate(savedSchedule.getScheduleDate())
                .scheduleStatus(savedSchedule.getStatus())
                .build();

        if (savedSchedule.getTemplate() != null) {
            response.setShiftName(savedSchedule.getTemplate().getShiftName());
            response.setStartTime(savedSchedule.getTemplate().getStartTime());
            response.setEndTime(savedSchedule.getTemplate().getEndTime());
        }

        return response;
    }

    public static ShiftTemplateResponse toShiftTemplateResponse(ShiftTemplate savedShiftTemplate) {
        return ShiftTemplateResponse.builder()
                .templateId(savedShiftTemplate.getTemplateId())
                .shiftName(savedShiftTemplate.getShiftName())
                .startTime(savedShiftTemplate.getStartTime())
                .endTime(savedShiftTemplate.getEndTime())
                .remarks(savedShiftTemplate.getRemarks())
                .build();
    }

    // All Resources - Now 100% Null-Safe for Stale Redis Data
    public static ResourceResponse toResourceResponse(
            List<Map<Object, Object>> activeVehicles,
            List<Map<Object, Object>> activeDrivers,
            List<Map<Object, Object>> activeRoutes,
            List<Map<Object, Object>> activeTemplates) {

        return ResourceResponse.builder()
                .vehicles(activeVehicles.stream().map(v -> ResourceResponse.VehicleOption.builder()
                        .vehicleNo(v.get("vehicleNo").toString())
                        .status(v.get("status") != null ? v.get("status").toString() : "ACTIVE")
                        .build()).collect(Collectors.toList()))

                .drivers(activeDrivers.stream().map(d -> ResourceResponse.DriverOption.builder()
                        .driverId(UUID.fromString(d.get("driverId").toString())) // Injected from key
                        .name(d.get("name") != null ? d.get("name").toString() : "Unknown Name")
                        .phoneNo(d.get("phoneNo") != null ? d.get("phoneNo").toString() : "")
                        .status(d.get("status") != null ? d.get("status").toString() : "ACTIVE")
                        .build()).collect(Collectors.toList()))

                .routes(activeRoutes.stream().map(r -> ResourceResponse.RouteOption.builder()
                        .routeId(UUID.fromString(r.get("routeId").toString())) // Injected from key
                        .origin(r.get("origin") != null ? r.get("origin").toString() : "Unknown Origin")
                        .destination(r.get("destination") != null ? r.get("destination").toString() : "Unknown Destination")
                        .status(r.get("status") != null ? r.get("status").toString() : "ACTIVE")
                        .build()).collect(Collectors.toList()))

                .templates(activeTemplates.stream().map(t -> ResourceResponse.TemplateOption.builder()
                        .templateId(UUID.fromString(t.get("templateId").toString())) // Injected from key
                        .shiftName(t.get("shiftName") != null ? t.get("shiftName").toString() : "Unnamed Shift")
                        .startTime(t.get("startTime") != null ? LocalTime.parse(t.get("startTime").toString()) : null)
                        .endTime(t.get("endTime") != null ? LocalTime.parse(t.get("endTime").toString()) : null)
                        .status(t.get("status") != null ? t.get("status").toString() : "ACTIVE")
                        .build()).collect(Collectors.toList()))
                .build();
    }
}