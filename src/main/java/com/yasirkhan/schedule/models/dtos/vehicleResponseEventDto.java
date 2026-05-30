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
public class vehicleResponseEventDto {

    private String vehicleNo;
    // private String type; // CREATE , UPDATE
    private String eventTypeStatus; // SUCCESS, FAILURE

    // status
    private String status;
}
