package com.yasirkhan.schedule.models.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yasirkhan.schedule.models.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VehicleResponseEventDto {

    private EventStatus eventTypeStatus; // SUCCESS, FAILURE
    private String vehicleNo;
    private String status;

    /**
     * This method acts as a bridge. When Jackson parses the JSON,
     * it grabs the nested "vehicleData" object and runs this method,
     * flattening the data into your fields.
     */
    @JsonProperty("vehicleData")
    private void unpackNestedVehicleData(Map<String, Object> vehicleData) {
        if (vehicleData != null) {
            this.vehicleNo = (String) vehicleData.get("vehicleNo");
            this.status = (String) vehicleData.get("status");
        }
    }
}