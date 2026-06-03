package com.yasirkhan.schedule.models.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yasirkhan.schedule.models.enums.EventStatus;
import com.yasirkhan.schedule.models.enums.EventType;
import com.yasirkhan.schedule.responses.ScheduleResponse;
import com.yasirkhan.schedule.responses.ShiftTemplateResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShiftTemplateResponseEventDto {

    private EventType type;
    private EventStatus eventTypeStatus;
    private ShiftTemplateResponse templateData;
}
