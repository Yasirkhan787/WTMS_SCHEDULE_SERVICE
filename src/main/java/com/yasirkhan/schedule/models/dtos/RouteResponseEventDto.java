package com.yasirkhan.schedule.models.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RouteResponseEventDto {

    private UUID routeId;

    // private String type; // CREATE , UPDATE

    private String eventTypeStatus; // SUCCESS, FAILURE

    private String origin;

    private String destination;

    private String status;  // (ACTUAL STATUS e.g., ACTIVE, BLOCKED etc...)

    //private String message;
}
