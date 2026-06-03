package com.yasirkhan.schedule.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AvailableAssetRequest {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate scheduleDate;
    private UUID shiftTemplateId;
    private UUID routeId;
}