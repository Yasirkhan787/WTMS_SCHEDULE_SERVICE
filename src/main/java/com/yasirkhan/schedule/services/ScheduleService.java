package com.yasirkhan.schedule.services;

import com.yasirkhan.schedule.requests.ScheduleRequest;
import com.yasirkhan.schedule.responses.ScheduleResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ScheduleService {

    ScheduleResponse createSchedule(ScheduleRequest request);

    void updateSchedule(Map<String, Object> updates);

    ScheduleResponse getScheduleById(UUID scheduleId);

    List<ScheduleResponse> getAllSchedule();
}
