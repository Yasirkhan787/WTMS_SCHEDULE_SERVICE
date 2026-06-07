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
}