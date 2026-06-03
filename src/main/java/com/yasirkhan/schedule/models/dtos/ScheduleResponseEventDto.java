package com.yasirkhan.schedule.models.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yasirkhan.schedule.models.enums.EventStatus;
import com.yasirkhan.schedule.models.enums.EventType;
import com.yasirkhan.schedule.responses.ScheduleResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleResponseEventDto {

    private EventType type;
    private EventStatus eventTypeStatus;
    private ScheduleResponse scheduleData;
}
