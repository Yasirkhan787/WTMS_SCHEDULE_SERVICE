package com.yasirkhan.schedule.models.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yasirkhan.schedule.models.enums.EventStatus;
import com.yasirkhan.schedule.models.enums.EventType;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TripResponseEventDto {
    private EventType type;
    private EventStatus eventTypeStatus;
    private TripResponse tripData;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TripResponse {
        private UUID tripId;
        private UUID scheduleId;
        private String slipId;
        private String vehicleNo;
        private String loadTime;
        private Double loadWeight;
        private Double actualDistance;
        private String emptyTime;
        private Double emptyWeight;
        private Double netWeight;
        private Double fuelConsumed;
        private String status;
    }
}